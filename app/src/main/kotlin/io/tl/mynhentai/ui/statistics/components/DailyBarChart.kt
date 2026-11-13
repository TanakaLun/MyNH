package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.DailyUsage
import io.tl.mynhentai.ui.statistics.formatDay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DailyBarChart(
    daily: List<DailyUsage>,
    rangeDays: Int,
    modifier: Modifier = Modifier,
) {
    val data = remember(daily, rangeDays) { daily.takeLast(rangeDays) }
    val maxMinutes = remember(data) { data.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1) ?: 1 }
    val barColor = MiuixTheme.colorScheme.primary
    val trackColor = MiuixTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        if (data.none { it.hasActivity }) {
            Text(
                text = stringResource(R.string.stats_daily_empty),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                val slot = size.width / data.size
                val barWidth = (slot * 0.62f).coerceAtLeast(1f)
                val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                data.forEachIndexed { index, day ->
                    val left = index * slot + (slot - barWidth) / 2f
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = radius
                    )
                    val ratio = day.totalMinutes.toFloat() / maxMinutes
                    val barHeight = size.height * ratio.coerceIn(0f, 1f)
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.firstOrNull()?.let { formatDay(it.date) } ?: "",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = data.lastOrNull()?.let { formatDay(it.date) } ?: "",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}
