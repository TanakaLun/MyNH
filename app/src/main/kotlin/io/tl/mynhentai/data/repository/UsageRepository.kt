package io.tl.mynhentai.data.repository

import io.tl.mynhentai.data.local.ReadingRecordEntity
import io.tl.mynhentai.data.local.UsageDao
import io.tl.mynhentai.data.local.UsageEventEntity
import io.tl.mynhentai.data.model.ActivityItem
import io.tl.mynhentai.data.model.DailyUsage
import io.tl.mynhentai.data.model.HabitStats
import io.tl.mynhentai.data.model.SearchTerm
import io.tl.mynhentai.data.model.TagGroup
import io.tl.mynhentai.data.model.TagPreference
import io.tl.mynhentai.data.model.TagSnapshot
import io.tl.mynhentai.data.model.UsageKind
import io.tl.mynhentai.data.model.UsageStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Aggregates the raw usage tables into [UsageStats] for the statistics screen.
 *
 * Everything is computed locally in memory: the 400-day window holds at most a few
 * thousand rows, which is cheaper than maintaining extra aggregate tables.
 */
class UsageRepository(private val dao: UsageDao) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val json = Json { ignoreUnknownKeys = true }

    fun observeStats(): Flow<UsageStats> {
        val since = System.currentTimeMillis() - RETENTION_DAYS * MILLIS_PER_DAY
        return combine(
            dao.observeEventsSince(since),
            dao.observeReadingsSince(since),
        ) { events, readings -> buildStats(events, readings) }
    }

    suspend fun clearAll() {
        dao.clearEvents()
        dao.clearReadings()
    }

    suspend fun snapshot(): Pair<List<UsageEventEntity>, List<ReadingRecordEntity>> =
        dao.getAllEventsOnce() to dao.getAllReadingsOnce()

    suspend fun restore(events: List<UsageEventEntity>, readings: List<ReadingRecordEntity>) {
        dao.clearEvents()
        dao.clearReadings()
        if (events.isNotEmpty()) dao.insertEvents(events)
        if (readings.isNotEmpty()) dao.insertReadings(readings)
    }

    private fun buildStats(
        events: List<UsageEventEntity>,
        readings: List<ReadingRecordEntity>,
    ): UsageStats {
        val today = LocalDate.now(zone)
        val firstDay = today.minusDays((WINDOW_DAYS - 1).toLong())

        val dayAccum = HashMap<LocalDate, DayAccum>()
        fun accumOf(date: LocalDate): DayAccum? {
            if (date.isBefore(firstDay) || date.isAfter(today)) return null
            return dayAccum.getOrPut(date) { DayAccum() }
        }

        events.forEach { event ->
            val acc = accumOf(dateOf(event.startAt)) ?: return@forEach
            when (event.kind) {
                UsageKind.APP -> acc.appMillis += event.durationMillis
                UsageKind.BROWSE -> acc.browseCount++
                UsageKind.SEARCH -> acc.searchCount++
            }
        }
        readings.forEach { record ->
            val acc = accumOf(dateOf(record.startedAt)) ?: return@forEach
            acc.readingMillis += record.durationMillis
        }

        val daily = (0 until WINDOW_DAYS).map { offset ->
            val date = firstDay.plusDays(offset.toLong())
            val acc = dayAccum[date] ?: DayAccum()
            DailyUsage(
                date = date,
                appMillis = acc.appMillis,
                readingMillis = acc.readingMillis,
                browseCount = acc.browseCount,
                searchCount = acc.searchCount,
            )
        }

        val totalMillis = daily.sumOf { it.totalMillis }
        val readingTotal = daily.sumOf { it.readingMillis }
        val appTotal = daily.sumOf { it.appMillis }

        val tagAccum = HashMap<Long, TagAccum>()
        val hourHistogram = IntArray(24)
        var readingDurationSum = 0L
        var pageSum = 0L
        readings.forEach { record ->
            readingDurationSum += record.durationMillis
            pageSum += record.lastPage.coerceAtLeast(1)
            hourHistogram[Instant.ofEpochMilli(record.startedAt).atZone(zone).hour]++
            parseTags(record.tagsJson).forEach { tag ->
                val acc = tagAccum.getOrPut(tag.id) { TagAccum(tag.id, tag.name, tag.type) }
                acc.count++
                acc.millis += record.durationMillis
            }
        }

        val totalTagCount = tagAccum.values.sumOf { it.count }.coerceAtLeast(1)
        val totalTagMillis = tagAccum.values.sumOf { it.millis }.coerceAtLeast(1L)
        val rawPreferences = tagAccum.values.map { acc ->
            val weight = (acc.millis.toFloat() / totalTagMillis) * 0.6f +
                (acc.count.toFloat() / totalTagCount) * 0.4f
            TagPreference(acc.id, acc.name, acc.type, acc.count, acc.millis, weight)
        }
        val maxWeight = rawPreferences.maxOfOrNull { it.score }?.takeIf { it > 0f } ?: 1f
        val preferences = rawPreferences
            .map { it.copy(score = (it.score / maxWeight).coerceIn(0f, 1f)) }
            .sortedByDescending { it.score }

        val tagGroups = preferences
            .groupBy { it.type }
            .toList()
            .sortedBy { (type, _) ->
                val index = TAG_TYPE_ORDER.indexOf(type)
                if (index < 0) TAG_TYPE_ORDER.size else index
            }
            .map { (type, tags) -> TagGroup(type, tags.take(3)) }

        val searches = events.filter { it.kind == UsageKind.SEARCH }
        val topSearches = searches
            .mapNotNull { it.query?.trim()?.takeIf { query -> query.isNotEmpty() } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(TOP_SEARCHES)
            .map { SearchTerm(it.key, it.value) }
        val recentSearches = searches.take(RECENT_SEARCHES).map { event ->
            ActivityItem(
                kind = UsageKind.SEARCH,
                at = event.startAt,
                query = event.query,
            )
        }

        val trail = buildList {
            readings.forEach { record ->
                add(
                    ActivityItem(
                        kind = UsageKind.READ,
                        at = record.startedAt,
                        title = record.title,
                        galleryId = record.galleryId,
                        coverPath = record.coverPath,
                        durationMillis = record.durationMillis,
                    )
                )
            }
            events.forEach { event ->
                when (event.kind) {
                    UsageKind.BROWSE -> add(
                        ActivityItem(
                            kind = UsageKind.BROWSE,
                            at = event.startAt,
                            title = event.galleryTitle,
                            galleryId = event.galleryId,
                            coverPath = event.coverPath,
                        )
                    )
                    UsageKind.SEARCH -> add(
                        ActivityItem(
                            kind = UsageKind.SEARCH,
                            at = event.startAt,
                            query = event.query,
                        )
                    )
                }
            }
        }.sortedByDescending { it.at }.take(TRAIL_LIMIT)

        val activeDays = daily.filter { it.hasActivity }.map { it.date }.toSet()
        var cursor = today
        if (!activeDays.contains(cursor)) cursor = today.minusDays(1)
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val peakHour = hourHistogram.indices
            .maxByOrNull { hourHistogram[it] }
            ?.takeIf { hourHistogram[it] > 0 }

        val habits = HabitStats(
            totalReadings = readings.size,
            distinctGalleries = readings.map { it.galleryId }.toSet().size,
            averageSessionMillis = if (readings.isEmpty()) 0L else readingDurationSum / readings.size,
            averageMillisPerPage = if (pageSum <= 0L) 0L else readingDurationSum / pageSum,
            peakHour = peakHour,
            streakDays = streak,
        )

        return UsageStats(
            todayMillis = daily.lastOrNull()?.totalMillis ?: 0L,
            weekMillis = daily.takeLast(7).sumOf { it.totalMillis },
            monthMillis = daily.takeLast(30).sumOf { it.totalMillis },
            totalMillis = totalMillis,
            readingMillis = readingTotal,
            appMillis = appTotal,
            daily = daily,
            habits = habits,
            topTags = preferences.take(TOP_TAGS),
            tagGroups = tagGroups,
            topSearches = topSearches,
            recentSearches = recentSearches,
            trail = trail,
        )
    }

    private fun dateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun parseTags(tagsJson: String): List<TagSnapshot> =
        runCatching {
            json.decodeFromString(ListSerializer(TagSnapshot.serializer()), tagsJson)
                .distinctBy { it.id }
        }.getOrDefault(emptyList())

    private class DayAccum {
        var appMillis: Long = 0
        var readingMillis: Long = 0
        var browseCount: Int = 0
        var searchCount: Int = 0
    }

    private class TagAccum(val id: Long, val name: String, val type: String) {
        var count: Int = 0
        var millis: Long = 0
    }

    private companion object {
        const val RETENTION_DAYS = 400L
        const val WINDOW_DAYS = 365
        const val MILLIS_PER_DAY = 86_400_000L
        const val TOP_TAGS = 12
        const val TOP_SEARCHES = 12
        const val RECENT_SEARCHES = 10
        const val TRAIL_LIMIT = 60

        val TAG_TYPE_ORDER = listOf(
            "parody",
            "character",
            "artist",
            "group",
            "language",
            "category",
            "tag",
        )
    }
}
