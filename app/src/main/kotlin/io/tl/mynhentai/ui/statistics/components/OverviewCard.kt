package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.UsageStats
import io.tl.mynhentai.ui.statistics.formatDuration
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverviewCard(
    stats: UsageStats,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(
                    label = stringResource(R.string.stats_today),
                    value = formatDuration(stats.todayMillis),
                    modifier = Modifier.weight(1f)
                )
                Metric(
                    label = stringResource(R.string.stats_week),
                    value = formatDuration(stats.weekMillis),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(
                    label = stringResource(R.string.stats_month),
                    value = formatDuration(stats.monthMillis),
                    modifier = Modifier.weight(1f)
                )
                Metric(
                    label = stringResource(R.string.stats_total),
                    value = formatDuration(stats.totalMillis),
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val ratio = stats.readingRatio.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                ) {
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MiuixTheme.colorScheme.primary)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.stats_reading_ratio,
                            (ratio * 100).toInt()
                        ),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = formatDuration(stats.readingMillis),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.stats_streak_summary,
                    stats.habits.streakDays,
                    stats.habits.distinctGalleries
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSecondaryContainer
        )
    }
}
