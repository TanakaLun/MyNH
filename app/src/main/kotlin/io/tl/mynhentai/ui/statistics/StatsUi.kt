package io.tl.mynhentai.ui.statistics

import androidx.annotation.StringRes
import io.tl.mynhentai.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

/** Compact duration, e.g. "42m", "1h 05m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    return when {
        totalMinutes <= 0L -> "0m"
        totalMinutes < 60L -> "${totalMinutes}m"
        else -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
        }
    }
}

fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(clockFormatter)

fun formatDay(date: LocalDate): String = date.format(dayFormatter)

fun formatMonth(date: LocalDate): String = date.format(monthFormatter)

fun peakPeriodOf(hour: Int): PeakPeriod = when (hour) {
    in 5..11 -> PeakPeriod.MORNING
    in 12..17 -> PeakPeriod.AFTERNOON
    in 18..22 -> PeakPeriod.EVENING
    else -> PeakPeriod.NIGHT
}

enum class PeakPeriod(@StringRes val labelRes: Int) {
    MORNING(R.string.stats_period_morning),
    AFTERNOON(R.string.stats_period_afternoon),
    EVENING(R.string.stats_period_evening),
    NIGHT(R.string.stats_period_night),
}

@StringRes
fun tagTypeLabelRes(type: String): Int = when (type) {
    "parody" -> R.string.tag_type_parody
    "character" -> R.string.tag_type_character
    "artist" -> R.string.tag_type_artist
    "group" -> R.string.tag_type_group
    "language" -> R.string.tag_type_language
    "category" -> R.string.tag_type_category
    else -> R.string.tag_type_tag
}

@StringRes
fun activityKindLabelRes(kind: String): Int = when (kind) {
    io.tl.mynhentai.data.model.UsageKind.READ -> R.string.stats_activity_read
    io.tl.mynhentai.data.model.UsageKind.BROWSE -> R.string.stats_activity_browse
    io.tl.mynhentai.data.model.UsageKind.SEARCH -> R.string.stats_activity_search
    else -> R.string.stats_activity_app
}
