package io.tl.mynhentai.ui.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadService : android.app.Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var client: OkHttpClient

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
                        .header("User-Agent", "MyNHentai/1.0")
                        .build()
                )
            }
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                @Suppress("UNCHECKED_CAST")
                val pages = intent.getSerializableExtra(EXTRA_PAGES) as? List<Pair<Int, String>> ?: return START_NOT_STICKY
                val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "gallery_$galleryId"
                val targetDir = intent.getStringExtra(EXTRA_TARGET_DIR) ?: return START_NOT_STICKY
                scope.launch {
                    doDownload(pages, galleryId, title, targetDir)
                    stopSelf()
                }
            }
            ACTION_CACHE -> {
                @Suppress("UNCHECKED_CAST")
                val pages = intent.getSerializableExtra(EXTRA_PAGES) as? List<Pair<Int, String>> ?: return START_NOT_STICKY
                val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "gallery_$galleryId"
                scope.launch {
                    doCache(pages, galleryId, title)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?) = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun doDownload(
        pages: List<Pair<Int, String>>,
        galleryId: Long,
        title: String,
        targetDir: String
    ) = withContext(Dispatchers.IO) {
        val tempDir = File(cacheDir, "download_temp_$galleryId")
        tempDir.mkdirs()
        try {
            startForeground(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildProgressNotification(this@DownloadService, title, 0, pages.size)
            )
            for ((i, pair) in pages.withIndex()) {
                val (num, url) = pair
                val ext = if (url.contains("jpg") || url.contains("jpeg")) ".jpg" else ".png"
                val pageFile = File(tempDir, "$num$ext")
                if (!pageFile.exists()) {
                    downloadImage(url, pageFile)
                }
                NotificationManagerCompat.from(this@DownloadService).notify(
                    NotificationHelper.PROGRESS_NOTIFICATION_ID,
                    NotificationHelper.buildProgressNotification(this@DownloadService, title, i + 1, pages.size)
                )
            }
            val zipFile = File(targetDir, "${title.replace("/", "_")}.zip")
            zipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                tempDir.listFiles()?.sortedBy { it.nameWithoutExtension.toIntOrNull() }?.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            tempDir.deleteRecursively()
            NotificationManagerCompat.from(this@DownloadService).notify(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildSuccessNotification(this@DownloadService, title)
            )
            stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            NotificationManagerCompat.from(this@DownloadService).notify(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildFailedNotification(this@DownloadService, title, e.message ?: "未知错误")
            )
            stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        }
    }

    private suspend fun doCache(
        pages: List<Pair<Int, String>>,
        galleryId: Long,
        title: String
    ) = withContext(Dispatchers.IO) {
        val offlineDir = File(this@DownloadService.cacheDir, "offline/$galleryId")
        offlineDir.mkdirs()
        try {
            startForeground(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildProgressNotification(this@DownloadService, title, 0, pages.size)
            )
            var count = 0
            for ((i, pair) in pages.withIndex()) {
                val (num, url) = pair
                val ext = if (url.contains("jpg") || url.contains("jpeg")) ".jpg" else ".png"
                val pageFile = File(offlineDir, "$num$ext")
                if (!pageFile.exists()) {
                    try {
                        downloadImage(url, pageFile)
                        count++
                    } catch (_: Exception) { }
                }
                NotificationManagerCompat.from(this@DownloadService).notify(
                    NotificationHelper.PROGRESS_NOTIFICATION_ID,
                    NotificationHelper.buildProgressNotification(this@DownloadService, title, i + 1, pages.size)
                )
            }
            NotificationManagerCompat.from(this@DownloadService).notify(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildSuccessNotification(this@DownloadService, "已缓存 $count 页")
            )
            stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            NotificationManagerCompat.from(this@DownloadService).notify(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                NotificationHelper.buildFailedNotification(this@DownloadService, title, e.message ?: "缓存失败")
            )
            stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        }
    }

    private fun downloadImage(url: String, dest: File) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.body?.byteStream()?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        const val ACTION_DOWNLOAD = "io.tl.mynhentai.action.DOWNLOAD"
        const val ACTION_CACHE = "io.tl.mynhentai.action.CACHE"
        const val EXTRA_PAGES = "pages"
        const val EXTRA_GALLERY_ID = "gallery_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TARGET_DIR = "target_dir"

        fun startDownload(
            context: Context,
            pages: List<Pair<Int, String>>,
            galleryId: Long,
            title: String,
            targetDir: String
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_PAGES, ArrayList(pages))
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
            title: String
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CACHE
                putExtra(EXTRA_PAGES, ArrayList(pages))
                putExtra(EXTRA_GALLERY_ID, galleryId)
                putExtra(EXTRA_TITLE, title)
            }
            context.startForegroundService(intent)
        }
    }
}
