package io.tl.mynhentai.data.tracker

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.tl.mynhentai.data.local.ReadingRecordEntity
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.local.UsageDao
import io.tl.mynhentai.data.local.UsageEventEntity
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.TagHelper
import io.tl.mynhentai.data.model.TagSnapshot
import io.tl.mynhentai.data.model.UsageKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Records how long the app is in the foreground, how long each gallery is read and
 * which galleries/searches are visited.
 *
 * Sessions are checkpointed to the database every [CHECKPOINT_INTERVAL] ms so that a
 * process kill loses at most one interval of data. All writes are no-ops while
 * statistics are disabled in the settings.
 */
class UsageTracker(
    private val dao: UsageDao,
    private val settings: SettingsHelper,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile private var tickerJob: Job? = null

    @Volatile private var appToken = 0L
    @Volatile private var appSessionId: Long? = null
    @Volatile private var appSessionStartAt = 0L
    @Volatile private var appAccumulated = 0L
    @Volatile private var appSegmentStart = 0L

    @Volatile private var readingToken = 0L
    @Volatile private var readingSessionId: Long? = null
    @Volatile private var readingTemplate: ReadingRecordEntity? = null
    @Volatile private var readingAccumulated = 0L
    @Volatile private var readingSegmentStart = 0L
    @Volatile private var readingActive = false
    @Volatile private var readingPage = 1

    private val enabled: Boolean
        get() = settings.statisticsEnabled

    fun install() {
        prune()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = onForeground()

            override fun onStop(owner: LifecycleOwner) = onBackground()
        })
    }

    // region foreground app time

    private fun onForeground() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (appSegmentStart == 0L) {
            if (appSessionStartAt == 0L) appSessionStartAt = now
            appSegmentStart = now
        }
        if (readingActive && readingSegmentStart == 0L) readingSegmentStart = now
        startTicker()
    }

    private fun onBackground() {
        closeAppSegment()
        closeReadingSegment()
        persistApp(reset = true)
        persistReading(reset = false)
        stopTicker()
    }

    private fun closeAppSegment() {
        val start = appSegmentStart
        if (start != 0L) {
            appAccumulated += (System.currentTimeMillis() - start).coerceAtLeast(0L)
            appSegmentStart = 0L
        }
    }

    private fun persistApp(reset: Boolean) {
        val id = appSessionId
        val startAt = appSessionStartAt
        val duration = appAccumulated
        val token = appToken
        if (reset) {
            appToken++
            appSessionId = null
            appSessionStartAt = 0L
            appAccumulated = 0L
        }
        if (id == null && duration < MIN_COMMIT_MILLIS) return
        launchSafe {
            if (id == null) {
                val newId = dao.insertEvent(
                    UsageEventEntity(
                        kind = UsageKind.APP,
                        startAt = startAt,
                        durationMillis = duration,
                    )
                )
                if (!reset && appToken == token) appSessionId = newId
            } else {
                dao.updateEventDuration(id, duration, startAt)
            }
        }
    }

    // endregion

    // region reading sessions

    fun startReading(detail: MangaDetail) {
        if (!enabled) return
        stopReading()
        val now = System.currentTimeMillis()
        val title = detail.title.pretty ?: detail.title.english ?: detail.title.japanese ?: ""
        val tags = detail.tags.map { TagSnapshot(it.id, it.name, it.type) }
        val tagsJson = runCatching {
            json.encodeToString(ListSerializer(TagSnapshot.serializer()), tags)
        }.getOrDefault("[]")
        val language = TagHelper.getLanguage(detail.tags.map { it.id.toInt() })

        readingToken++
        readingSessionId = null
        readingTemplate = ReadingRecordEntity(
            galleryId = detail.id,
            title = title,
            coverPath = detail.cover.path,
            startedAt = now,
            durationMillis = 0L,
            pageCount = detail.pages.size,
            lastPage = 1,
            tagsJson = tagsJson,
            language = language,
        )
        readingAccumulated = 0L
        readingPage = 1
        readingActive = true
        readingSegmentStart = now
    }

    fun updateReadingPage(page: Int) {
        if (readingActive && page > 0) readingPage = page
    }

    fun stopReading(expectedGalleryId: Long? = null) {
        if (!readingActive) return
        if (expectedGalleryId != null && readingTemplate?.galleryId != expectedGalleryId) return
        closeReadingSegment()
        persistReading(reset = true)
        readingActive = false
        readingTemplate = null
        readingAccumulated = 0L
        readingPage = 1
        readingSegmentStart = 0L
    }

    private fun closeReadingSegment() {
        val start = readingSegmentStart
        if (start != 0L) {
            readingAccumulated += (System.currentTimeMillis() - start).coerceAtLeast(0L)
            readingSegmentStart = 0L
        }
    }

    private fun persistReading(reset: Boolean) {
        val template = readingTemplate ?: return
        val id = readingSessionId
        val duration = readingAccumulated
        val page = readingPage
        val token = readingToken
        if (reset) {
            readingToken++
            readingSessionId = null
        }
        if (id == null && duration < MIN_COMMIT_MILLIS) return
        launchSafe {
            if (id == null) {
                val newId = dao.insertReading(
                    template.copy(durationMillis = duration, lastPage = page)
                )
                if (!reset && readingToken == token) readingSessionId = newId
            } else {
                dao.updateReading(id, duration, page)
            }
        }
    }

    // endregion

    // region instant events

    fun recordBrowse(detail: MangaDetail) {
        if (!enabled) return
        val title = detail.title.pretty ?: detail.title.english ?: detail.title.japanese ?: ""
        launchSafe {
            dao.insertEvent(
                UsageEventEntity(
                    kind = UsageKind.BROWSE,
                    startAt = System.currentTimeMillis(),
                    galleryId = detail.id,
                    galleryTitle = title,
                    coverPath = detail.cover.path,
                )
            )
        }
    }

    fun recordSearch(query: String) {
        if (!enabled || !settings.statisticsRecordQueries) return
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.isEmpty()) return
        launchSafe {
            dao.insertEvent(
                UsageEventEntity(
                    kind = UsageKind.SEARCH,
                    startAt = System.currentTimeMillis(),
                    query = trimmed,
                )
            )
        }
    }

    // endregion

    fun clearAll() {
        launchSafe {
            dao.clearEvents()
            dao.clearReadings()
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL)
                checkpoint()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun checkpoint() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (appSegmentStart != 0L) {
            appAccumulated += (now - appSegmentStart).coerceAtLeast(0L)
            appSegmentStart = now
        }
        persistApp(reset = false)
        if (readingActive && readingSegmentStart != 0L) {
            readingAccumulated += (now - readingSegmentStart).coerceAtLeast(0L)
            readingSegmentStart = now
            persistReading(reset = false)
        }
    }

    private fun prune() {
        launchSafe {
            val cutoff = System.currentTimeMillis() - RETENTION_DAYS * MILLIS_PER_DAY
            dao.deleteEventsBefore(cutoff)
            dao.deleteReadingsBefore(cutoff)
        }
    }

    private fun launchSafe(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Statistics must never crash the app.
            }
        }
    }

    private companion object {
        const val CHECKPOINT_INTERVAL = 30_000L
        const val MIN_COMMIT_MILLIS = 1_000L
        const val RETENTION_DAYS = 400L
        const val MILLIS_PER_DAY = 86_400_000L
        const val MAX_QUERY_LENGTH = 120
    }
}
