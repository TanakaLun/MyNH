package io.tl.mynhentai.ui.components

import android.app.Notification
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.DownloadCompleted
import io.tl.mynhentai.data.local.DownloadError
import io.tl.mynhentai.data.local.DownloadKind
import io.tl.mynhentai.data.local.DownloadStateHolder
import io.tl.mynhentai.data.local.SettingsHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.koin.android.ext.android.get

/**
 * Foreground service that owns all download / offline-cache tasks.
 *
 * Every incoming intent creates an independent task (with its own notification and job).
 * Tasks for the same (kind, galleryId) are deduplicated while one is still active.
 * The service stays foregrounded (with a partial wake lock) until the last task finishes,
 * so concurrent tasks never stop the process out from under each other.
 */
class DownloadService : Service() {

    private val lock = Any()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var client: OkHttpClient
    private lateinit var stateHolder: DownloadStateHolder
    private lateinit var wakeLock: PowerManager.WakeLock

    private val taskCounter = AtomicInteger(0)
    private val tasks = mutableMapOf<Int, ActiveTask>()
    private val taskJobs = mutableMapOf<Int, Job>()
    private val taskKeys = mutableMapOf<Pair<DownloadKind, Long>, Int>()

    private var concurrencySemaphore = Semaphore(10)
    private var foregroundActive = false

    override fun onCreate() {
        super.onCreate()
        client = OkHttpClient.Builder()
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequestsPerHost = 20
                maxRequests = 100
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "MyNHentai (+https://github.com/TanakaLun/MyNH)")
                        .build()
                )
            }
            .build()
        stateHolder = get()
        val prefs = getSharedPreferences(SettingsHelper.PREFS_NAME, Context.MODE_PRIVATE)
        val maxConcurrency = prefs.getInt(SettingsHelper.KEY_CONCURRENCY, 10).coerceIn(1, 30)
        concurrencySemaphore = Semaphore(maxConcurrency)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:download").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val pages = intent.extractPages() ?: return START_NOT_STICKY
                val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "gallery_$galleryId"
                val targetDir = intent.getStringExtra(EXTRA_TARGET_DIR) ?: return START_NOT_STICKY
                startTask(DownloadKind.DOWNLOAD, pages, galleryId, title, targetDir)
            }

            ACTION_CACHE -> {
                val pages = intent.extractPages() ?: return START_NOT_STICKY
                val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "gallery_$galleryId"
                startTask(DownloadKind.CACHE, pages, galleryId, title, null)
            }

            ACTION_ARCHIVE -> {
                val url = intent.getStringExtra(EXTRA_ARCHIVE_URL) ?: return START_NOT_STICKY
                val format = intent.getStringExtra(EXTRA_ARCHIVE_FORMAT) ?: "cbz"
                val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "gallery_$galleryId"
                val targetDir = intent.getStringExtra(EXTRA_TARGET_DIR) ?: return START_NOT_STICKY
                startTask(
                    kind = DownloadKind.ARCHIVE,
                    pages = emptyList(),
                    galleryId = galleryId,
                    title = title,
                    targetDir = targetDir,
                    archiveUrl = url,
                    archiveFormat = format,
                )
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------------
    // Task orchestration
    // ---------------------------------------------------------------------------------------------

    private fun startTask(
        kind: DownloadKind,
        pages: List<Pair<Int, String>>,
        galleryId: Long,
        title: String,
        targetDir: String?,
        archiveUrl: String? = null,
        archiveFormat: String? = null,
    ) {
        synchronized(lock) {
            val key = kind to galleryId
            // Keep only one task per (kind, galleryId): a duplicate request is dropped.
            if (key in taskKeys) return

            val taskId = taskCounter.incrementAndGet()
            val task = ActiveTask(
                taskId = taskId,
                kind = kind,
                galleryId = galleryId,
                title = title,
                targetDir = targetDir,
                pages = pages,
                archiveUrl = archiveUrl,
                archiveFormat = archiveFormat,
            )

            taskKeys[key] = taskId
            tasks[taskId] = task

            if (tasks.size == 1) {
                // First active task: go foreground + hold a wake lock so the system won't
                // throttle the process while background work is still in flight.
                if (!foregroundActive) {
                    foregroundActive = true
                    startForeground(
                        NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                        NotificationHelper.buildForegroundNotification(
                            this, tasks.size, tasks.values.map { it.title }
                        )
                    )
                }
                acquireWakeLock()
            } else {
                refreshForegroundNotification()
            }

            val job = scope.launch {
                try {
                    runTask(task)
                } finally {
                    finishTask(taskId)
                }
            }
            taskJobs[taskId] = job
        }
    }

    private suspend fun runTask(task: ActiveTask) {
        var extraLine: String? = null
        val error: DownloadError? = try {
            when (task.kind) {
                DownloadKind.DOWNLOAD -> {
                    doDownload(task)
                    null
                }

                DownloadKind.CACHE -> {
                    val cached = doCache(task)
                    extraLine = getString(R.string.notification_cache_count, cached)
                    null
                }

                DownloadKind.ARCHIVE -> {
                    doArchive(task)
                    null
                }
            }
        } catch (e: FetchFailedException) {
            DownloadError(
                task.taskId, task.kind, task.galleryId, task.title,
                task.targetDir, task.pages, buildFailureMessage(e.failures),
                retryable = task.kind != DownloadKind.ARCHIVE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DownloadError(
                task.taskId, task.kind, task.galleryId, task.title,
                task.targetDir, task.pages, e.message?.takeIf { it.isNotBlank() } ?: getString(R.string.error_network),
                retryable = task.kind != DownloadKind.ARCHIVE,
            )
        }
        handleTaskResult(task, error, extraLine)
    }

    private fun handleTaskResult(task: ActiveTask, error: DownloadError?, extraLine: String?) {
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val notificationId = NotificationHelper.taskNotificationId(task.taskId)
        // The task progress notification is ongoing: remove it before showing the result.
        NotificationManagerCompat.from(this).cancel(notificationId)
        if (error != null) {
            stateHolder.reportError(error)
            if (!inForeground) {
                notifyTask(notificationId, NotificationHelper.buildFailedNotification(this, task.kind, task.title, error.message))
            }
        } else {
            if (inForeground) {
                stateHolder.reportCompletion(DownloadCompleted(task.kind, task.title))
            } else {
                notifyTask(notificationId, NotificationHelper.buildCompletionNotification(this, task.kind, task.title, extraLine))
            }
        }
    }

    private fun finishTask(taskId: Int) {
        synchronized(lock) {
            val task = tasks.remove(taskId)
            taskJobs.remove(taskId)
            if (task != null) taskKeys.remove(task.kind to task.galleryId)

            if (tasks.isEmpty()) {
                releaseWakeLock()
                if (foregroundActive) {
                    foregroundActive = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            } else {
                refreshForegroundNotification()
            }
        }
    }

    private fun refreshForegroundNotification() {
        val notification = NotificationHelper.buildForegroundNotification(
            this, tasks.size, tasks.values.map { it.title }
        )
        notifyTask(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            // Safety timeout so a stuck task can never hold the lock indefinitely.
            wakeLock.acquire(6 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun notifyTask(id: Int, notification: Notification) {
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    // ---------------------------------------------------------------------------------------------
    // Task workers
    // ---------------------------------------------------------------------------------------------

    private suspend fun doDownload(task: ActiveTask) {
        val tempDir = File(cacheDir, "download_temp_${task.galleryId}_${task.taskId}")
        tempDir.mkdirs()
        try {
            notifyProgress(task, 0, task.pages.size)
            val failures = fetchAllPages(task, tempDir) { it.second }
            if (failures.isNotEmpty()) throw FetchFailedException(failures)

            val uri = createDownloadUri("${safeFileName(task.title)}.zip", "application/zip")
            try {
                val output = contentResolver.openOutputStream(uri)
                    ?: throw IOException(getString(R.string.error_empty_response))
                output.use { stream ->
                    ZipOutputStream(stream).use { zos ->
                        tempDir.listFiles()
                            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() }
                            ?.forEach { file ->
                                zos.putNextEntry(ZipEntry(file.name))
                                file.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                    }
                }
                publishDownload(uri)
            } catch (e: Exception) {
                discardDownload(uri)
                throw e
            }
            tempDir.deleteRecursively()
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw e
        }
    }

    private suspend fun doCache(task: ActiveTask): Int {
        val dir = File(cacheDir, "offline/${task.galleryId}")
        dir.mkdirs()
        val failures = fetchAllPages(task, dir) { it.second }
        if (failures.isNotEmpty()) throw FetchFailedException(failures)
        return dir.listFiles()?.size ?: 0
    }

    /** Streams a server packaged archive (cbz/zip) from its signed URL. */
    private suspend fun doArchive(task: ActiveTask) {
        val url = task.archiveUrl ?: throw IOException(getString(R.string.error_empty_response))
        val format = task.archiveFormat?.takeIf { it in setOf("cbz", "zip") } ?: "cbz"
        val displayName = "${safeFileName(task.title)}.$format"
        val uri = createDownloadUri(displayName, "application/zip")

        notifyArchiveProgress(task, 0L, 0L)
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw DownloadHttpException(response.code)
                val body = response.body ?: throw IOException(getString(R.string.error_empty_response))
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                val output = contentResolver.openOutputStream(uri)
                    ?: throw IOException(getString(R.string.error_empty_response))
                output.use { stream ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        var lastNotifiedAt = 0L
                        var read = input.read(buffer)
                        while (read != -1) {
                            stream.write(buffer, 0, read)
                            done += read
                            val now = System.currentTimeMillis()
                            if (now - lastNotifiedAt >= NOTIFICATION_INTERVAL_MS) {
                                lastNotifiedAt = now
                                notifyArchiveProgress(task, done, totalBytes)
                            }
                            read = input.read(buffer)
                        }
                        notifyArchiveProgress(task, done, if (totalBytes > 0) totalBytes else done)
                    }
                }
            }
            publishDownload(uri)
        } catch (e: Exception) {
            discardDownload(uri)
            throw e
        }
    }

    private fun safeFileName(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "gallery" }

    private fun createDownloadUri(displayName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/MyNHentai"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException(getString(R.string.error_empty_response))
    }

    private fun publishDownload(uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        contentResolver.update(uri, values, null, null)
    }

    private fun discardDownload(uri: Uri) {
        runCatching { contentResolver.delete(uri, null, null) }
    }

    /**
     * Fetches every page concurrently, bounded by the per-task [concurrencySemaphore].
     * Already-present files are skipped (a retried task only fills in the gaps).
     * Returns the list of failed exceptions so callers can decide how to surface them.
     */
    private suspend fun fetchAllPages(
        task: ActiveTask,
        destDir: File,
        urlFor: (Pair<Int, String>) -> String,
    ): List<Throwable> {
        val completed = AtomicInteger(0)
        val failures = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        coroutineScope {
            task.pages.map { pair ->
                async {
                    val url = urlFor(pair)
                    val ext = if (url.contains("jpg") || url.contains("jpeg")) ".jpg" else ".png"
                    val pageFile = File(destDir, "${pair.first}$ext")
                    try {
                        concurrencySemaphore.withPermit {
                            if (!pageFile.exists()) downloadRaw(url, pageFile)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        failures.add(e)
                    } finally {
                        notifyProgress(task, completed.incrementAndGet(), task.pages.size)
                    }
                }
            }.awaitAll()
        }
        return failures
    }

    private fun downloadRaw(url: String, dest: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DownloadHttpException(response.code)
            }
            val body = response.body ?: throw IOException(getString(R.string.error_empty_response))
            body.byteStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun notifyProgress(task: ActiveTask, progress: Int, total: Int) {
        notifyTask(
            NotificationHelper.taskNotificationId(task.taskId),
            NotificationHelper.buildProgressNotification(this, task.title, progress, total)
        )
    }

    private fun notifyArchiveProgress(task: ActiveTask, doneBytes: Long, totalBytes: Long) {
        notifyTask(
            NotificationHelper.taskNotificationId(task.taskId),
            NotificationHelper.buildArchiveProgressNotification(
                this, task.title, doneBytes, totalBytes
            )
        )
    }



    private fun buildFailureMessage(failures: List<Throwable>): String {
        val httpFailure = failures.filterIsInstance<DownloadHttpException>().firstOrNull()
        val base = when {
            httpFailure != null -> httpFailure.userMessage(this)
            else -> failures.firstOrNull()?.message?.takeIf { it.isNotBlank() } ?: getString(R.string.error_network)
        }
        return if (failures.size > 1) {
            getString(R.string.error_failed_pages, base, failures.size)
        } else {
            base
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Models
    // ---------------------------------------------------------------------------------------------

    private class ActiveTask(
        val taskId: Int,
        val kind: DownloadKind,
        val galleryId: Long,
        val title: String,
        val targetDir: String?,
        val pages: List<Pair<Int, String>>,
        val archiveUrl: String? = null,
        val archiveFormat: String? = null,
    )

    private class DownloadHttpException(val code: Int) : IOException("HTTP $code") {
        fun userMessage(context: Context): String = when (code) {
            429 -> context.getString(R.string.error_http_429)
            else -> context.getString(R.string.error_http_status, code)
        }
    }

    private class FetchFailedException(val failures: List<Throwable>) : IOException()

    companion object {
        const val NOTIFICATION_INTERVAL_MS = 500L
        const val ACTION_DOWNLOAD = "io.tl.mynhentai.action.DOWNLOAD"
        const val ACTION_CACHE = "io.tl.mynhentai.action.CACHE"
        const val ACTION_ARCHIVE = "io.tl.mynhentai.action.ARCHIVE"
        const val EXTRA_PAGE_NUMBERS = "page_numbers"
        const val EXTRA_PAGE_URLS = "page_urls"
        const val EXTRA_GALLERY_ID = "gallery_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TARGET_DIR = "target_dir"
        const val EXTRA_ARCHIVE_URL = "archive_url"
        const val EXTRA_ARCHIVE_FORMAT = "archive_format"

        private fun Intent.extractPages(): List<Pair<Int, String>>? {
            val pageNumbers = getIntArrayExtra(EXTRA_PAGE_NUMBERS) ?: return null
            val pageUrls = getStringArrayExtra(EXTRA_PAGE_URLS) ?: return null
            return pageNumbers.zip(pageUrls.toList())
        }

        fun startDownload(
            context: Context,
            pages: List<Pair<Int, String>>,
            galleryId: Long,
            title: String,
            targetDir: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_PAGE_NUMBERS, pages.map { it.first }.toIntArray())
                putExtra(EXTRA_PAGE_URLS, pages.map { it.second }.toTypedArray())
                putExtra(EXTRA_GALLERY_ID, galleryId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TARGET_DIR, targetDir)
            }
            context.startForegroundService(intent)
        }

        fun startCache(
            context: Context,
            pages: List<Pair<Int, String>>,
            galleryId: Long,
            title: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CACHE
                putExtra(EXTRA_PAGE_NUMBERS, pages.map { it.first }.toIntArray())
                putExtra(EXTRA_PAGE_URLS, pages.map { it.second }.toTypedArray())
                putExtra(EXTRA_GALLERY_ID, galleryId)
                putExtra(EXTRA_TITLE, title)
            }
            context.startForegroundService(intent)
        }

        fun startArchive(
            context: Context,
            url: String,
            galleryId: Long,
            title: String,
            targetDir: String,
            format: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_ARCHIVE
                putExtra(EXTRA_ARCHIVE_URL, url)
                putExtra(EXTRA_ARCHIVE_FORMAT, format)
                putExtra(EXTRA_GALLERY_ID, galleryId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TARGET_DIR, targetDir)
            }
            context.startForegroundService(intent)
        }
    }
}