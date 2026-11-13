package io.tl.mynhentai.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

object UsageKind {
    const val APP = "app"
    const val BROWSE = "browse"
    const val SEARCH = "search"
    const val READ = "read"
}

/** Compact tag snapshot stored inside [io.tl.mynhentai.data.local.ReadingRecordEntity]. */
@Serializable
data class TagSnapshot(
    val id: Long,
    val name: String,
    val type: String,
)

data class DailyUsage(
    val date: LocalDate,
    val appMillis: Long = 0,
    val readingMillis: Long = 0,
    val browseCount: Int = 0,
    val searchCount: Int = 0,
) {
    val totalMillis: Long get() = appMillis + readingMillis
    val totalMinutes: Int get() = (totalMillis / 60_000L).toInt()
    val hasActivity: Boolean get() = totalMillis > 0 || browseCount > 0 || searchCount > 0
}

data class TagPreference(
    val id: Long,
    val name: String,
    val type: String,
    val readCount: Int,
    val totalMillis: Long,
    val score: Float,
)

data class TagGroup(
    val type: String,
    val tags: List<TagPreference>,
)

data class HabitStats(
    val totalReadings: Int = 0,
    val distinctGalleries: Int = 0,
    val averageSessionMillis: Long = 0,
    val averageMillisPerPage: Long = 0,
    val peakHour: Int? = null,
    val streakDays: Int = 0,
)

data class SearchTerm(
    val query: String,
    val count: Int,
)

data class ActivityItem(
    val kind: String,
    val at: Long,
    val title: String? = null,
    val galleryId: Long? = null,
    val coverPath: String? = null,
    val durationMillis: Long = 0,
    val query: String? = null,
)

data class UsageStats(
    val todayMillis: Long = 0,
    val weekMillis: Long = 0,
    val monthMillis: Long = 0,
    val totalMillis: Long = 0,
    val readingMillis: Long = 0,
    val appMillis: Long = 0,
    val daily: List<DailyUsage> = emptyList(),
    val habits: HabitStats = HabitStats(),
    val topTags: List<TagPreference> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val topSearches: List<SearchTerm> = emptyList(),
    val recentSearches: List<ActivityItem> = emptyList(),
    val trail: List<ActivityItem> = emptyList(),
) {
    val readingRatio: Float
        get() = if (totalMillis <= 0) 0f else readingMillis.toFloat() / totalMillis

    val isEmpty: Boolean
        get() = totalMillis <= 0 && topTags.isEmpty() && trail.isEmpty()
}
