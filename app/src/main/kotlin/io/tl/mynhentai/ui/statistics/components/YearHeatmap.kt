package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.DailyUsage
import io.tl.mynhentai.ui.statistics.formatDay
import io.tl.mynhentai.ui.statistics.formatDuration
import io.tl.mynhentai.ui.statistics.formatMonth
import java.time.LocalDate
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CellSize = 12.dp
private val CellGap = 3.dp
private val Step = CellSize + CellGap
private val MonthLabelHeight = 18.dp
private val CellShape = RoundedCornerShape(3.dp)

/**
 * GitHub style contribution grid: 53 Monday aligned week columns covering the last year.
 * Fixed thresholds (15/30/60 minutes) keep the color scale stable across days, and the grid
 * starts scrolled to the most recent week.
 */
@Composable
fun YearHeatmap(
    daily: List<DailyUsage>,
    modifier: Modifier = Modifier,
) {
    val today = daily.lastOrNull()?.date ?: LocalDate.now()
    val byDate = remember(daily) { daily.associateBy { it.date } }
    val firstDay = remember(today) {
        val from = today.minusDays(364)
        from.minusDays((from.dayOfWeek.value - 1).toLong())
    }
    val weeks = remember(firstDay, today) {
        buildList {
            var week = firstDay
            while (!week.isAfter(today)) {
                add(week)
                week = week.plusWeeks(1)
            }
        }
    }

    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) {
        if (scroll.maxValue > 0) scroll.scrollTo(scroll.maxValue)
    }

    val dark = isSystemInDarkTheme()
    val accent = MiuixTheme.colorScheme.primary
    val empty = MiuixTheme.colorScheme.outline
    val summaryColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    var selectedDate by remember { mutableStateOf(today) }
    val selectedUsage = byDate[selectedDate]

    Column(modifier) {
        Row {
            Column(Modifier.padding(top = MonthLabelHeight)) {
                for (index in 0 until 7) {
                    Box(Modifier.height(Step), contentAlignment = Alignment.CenterStart) {
                        if (index == 0 || index == 2 || index == 4) {
                            Text(
                                text = stringResource(
                                    when (index) {
                                        0 -> R.string.stats_weekday_mon
                                        2 -> R.string.stats_weekday_wed
                                        else -> R.string.stats_weekday_fri
                                    }
                                ),
                                fontSize = 10.sp,
                                color = summaryColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.horizontalScroll(scroll)) {
                Box(Modifier.width(Step * weeks.size).height(MonthLabelHeight)) {
                    weeks.forEachIndexed { index, week ->
                        val firstOfMonth = (0..6)
                            .map { week.plusDays(it.toLong()) }
                            .firstOrNull { it.dayOfMonth == 1 && !it.isAfter(today) }
                        if (firstOfMonth != null) {
                            Text(
                                text = formatMonth(firstOfMonth),
                                fontSize = 10.sp,
                                color = summaryColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.offset(x = Step * index)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                            for (dayOffset in 0 until 7) {
                                val date = week.plusDays(dayOffset.toLong())
                                if (date.isAfter(today)) {
                                    Spacer(Modifier.size(CellSize))
                                } else {
                                    val minutes = byDate[date]?.totalMinutes ?: 0
                                    val selected = date == selectedDate
                                    Box(
                                        modifier = Modifier
                                            .size(CellSize)
                                            .clip(CellShape)
                                            .background(heatColor(heatLevel(minutes), accent, empty, dark))
                                            .then(
                                                if (selected) {
                                                    Modifier.border(1.dp, MiuixTheme.colorScheme.onSurface, CellShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { selectedDate = date }
                                            .semantics {
                                                contentDescription = "$date ${formatDuration((byDate[date]?.totalMillis ?: 0L))}"
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.stats_heatmap_less),
                fontSize = 10.sp,
                color = summaryColor
            )
            Spacer(Modifier.width(6.dp))
            for (level in 0..4) {
                Box(
                    modifier = Modifier
                        .size(CellSize)
                        .clip(CellShape)
                        .background(heatColor(level, accent, empty, dark))
                )
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text(
                text = stringResource(R.string.stats_heatmap_more),
                fontSize = 10.sp,
                color = summaryColor
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.stats_heatmap_detail,
                formatDay(selectedDate),
                formatDuration(selectedUsage?.totalMillis ?: 0L),
                selectedUsage?.browseCount ?: 0
            ),
            style = MiuixTheme.textStyles.footnote2,
            color = summaryColor
        )
    }
}

private fun heatLevel(minutes: Int): Int = when {
    minutes <= 0 -> 0
    minutes < 15 -> 1
    minutes < 30 -> 2
    minutes < 60 -> 3
    else -> 4
}

private fun heatColor(level: Int, accent: Color, empty: Color, dark: Boolean): Color = when (level) {
    0 -> empty.copy(alpha = if (dark) 0.35f else 0.55f)
    1 -> accent.copy(alpha = 0.28f)
    2 -> accent.copy(alpha = 0.48f)
    3 -> accent.copy(alpha = 0.72f)
    else -> accent
}
