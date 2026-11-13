package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.DailyUsage
import io.tl.mynhentai.ui.statistics.formatDay
import io.tl.mynhentai.ui.statistics.formatDuration
import io.tl.mynhentai.ui.statistics.formatMonth
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate

private val CellSize = 11.dp
private val CellGap = 2.dp
private val WeekdayWidth = 26.dp

/**
 * GitHub style contribution grid: a fixed one year window (53 columns), scrolls horizontally
 * and starts at the most recent week.
 */
@Composable
fun YearHeatmap(
    daily: List<DailyUsage>,
    modifier: Modifier = Modifier,
) {
    val usageByDate = remember(daily) { daily.associateBy { it.date } }
    val lastDay = daily.lastOrNull()?.date ?: LocalDate.now()
    val firstDay = daily.firstOrNull()?.date ?: lastDay.minusDays(364)
    val lead = firstDay.dayOfWeek.value - 1
    val totalCells = lead + daily.size.coerceAtLeast(1)
    val columns = (totalCells + 6) / 7

    val maxMinutes = remember(daily) { daily.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(60) ?: 60 }

    val emptyColor = MiuixTheme.colorScheme.surfaceContainerHigh
    val primary = MiuixTheme.colorScheme.primary
    val textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    fun colorFor(minutes: Int): Color {
        if (minutes <= 0) return emptyColor
        val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0.1f, 1f)
        return lerp(emptyColor, primary, 0.3f + 0.7f * fraction)
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.maxValue }.first { it > 0 }
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Spacer(Modifier.width(WeekdayWidth))
            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                for (column in 0 until columns) {
                    val firstDateOfColumn = dateAt(column * 7, lead, firstDay, daily.lastIndex)
                    val previousDate = dateAt((column - 1) * 7, lead, firstDay, daily.lastIndex)
                    Box(modifier = Modifier.width(CellSize)) {
                        if (firstDateOfColumn != null &&
                            (column == 0 || previousDate == null ||
                                previousDate.month != firstDateOfColumn.month)
                        ) {
                            Text(
                                text = formatMonth(firstDateOfColumn),
                                fontSize = 9.sp,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Column(modifier = Modifier.width(WeekdayWidth)) {
                for (row in 0 until 7) {
                    Box(
                        modifier = Modifier.height(CellSize + CellGap),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (row == 0 || row == 2 || row == 4) {
                            Text(
                                text = stringResource(
                                    when (row) {
                                        0 -> R.string.stats_weekday_mon
                                        2 -> R.string.stats_weekday_wed
                                        else -> R.string.stats_weekday_fri
                                    }
                                ),
                                fontSize = 9.sp,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                for (column in 0 until columns) {
                    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (row in 0 until 7) {
                            val date = dateAt(column * 7 + row, lead, firstDay, daily.lastIndex)
                            val usage = date?.let { usageByDate[it] }
                            val minutes = usage?.totalMinutes ?: 0
                            Box(
                                modifier = Modifier
                                    .size(CellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colorFor(minutes))
                                    .then(
                                        if (date != null && usage != null && usage.hasActivity) {
                                            Modifier.clickable { selectedDate = date }
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(start = WeekdayWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_heatmap_less),
                style = MiuixTheme.textStyles.footnote2,
                color = textColor
            )
            for (level in 0..4) {
                Box(
                    modifier = Modifier
                        .size(CellSize)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (level == 0) emptyColor
                            else lerp(emptyColor, primary, 0.3f + 0.7f * (level / 4f))
                        )
                )
            }
            Text(
                text = stringResource(R.string.stats_heatmap_more),
                style = MiuixTheme.textStyles.footnote2,
                color = textColor
            )
        }

        val selected = selectedDate
        if (selected != null) {
            val usage = usageByDate[selected]
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.stats_heatmap_detail,
                    formatDay(selected),
                    formatDuration((usage?.readingMillis ?: 0) + (usage?.appMillis ?: 0)),
                    usage?.browseCount ?: 0
                ),
                style = MiuixTheme.textStyles.footnote2,
                color = textColor
            )
        }
    }
}

private fun dateAt(
    cellIndex: Int,
    lead: Int,
    firstDay: LocalDate,
    lastIndex: Int,
): LocalDate? {
    val dayIndex = cellIndex - lead
    if (dayIndex < 0 || dayIndex > lastIndex) return null
    return firstDay.plusDays(dayIndex.toLong())
}
