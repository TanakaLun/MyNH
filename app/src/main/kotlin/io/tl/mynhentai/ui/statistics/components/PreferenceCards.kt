package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.HabitStats
import io.tl.mynhentai.data.model.TagGroup
import io.tl.mynhentai.data.model.TagPreference
import io.tl.mynhentai.ui.statistics.formatDuration
import io.tl.mynhentai.ui.statistics.peakPeriodOf
import io.tl.mynhentai.ui.statistics.tagTypeLabelRes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

@Composable
fun TopTagsCard(
    topTags: List<TagPreference>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_top_tags),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface
            )
            if (topTags.isEmpty()) {
                EmptyHint()
            } else {
                topTags.forEachIndexed { index, tag ->
                    TagPreferenceRow(rank = index + 1, tag = tag)
                }
            }
        }
    }
}

@Composable
private fun TagPreferenceRow(rank: Int, tag: TagPreference) {
    val primary = MiuixTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$rank",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = tag.name,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${tag.readCount} · ${formatDuration(tag.totalMillis)}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant)
        ) {
            val fraction = tag.score.coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasteProfileCard(
    tagGroups: List<TagGroup>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_taste_profile),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface
            )
            if (tagGroups.isEmpty()) {
                EmptyHint()
            } else {
                tagGroups.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(tagTypeLabelRes(group.type)),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            group.tags.forEach { tag ->
                                TagChip(text = "${tag.name} ×${tag.readCount}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun HabitCard(
    habits: HabitStats,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_habits),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface
            )
            HabitRow(
                label = stringResource(R.string.stats_avg_session),
                value = formatDuration(habits.averageSessionMillis)
            )
            HabitRow(
                label = stringResource(R.string.stats_avg_per_page),
                value = formatDuration(habits.averageMillisPerPage)
            )
            HabitRow(
                label = stringResource(R.string.stats_peak_hour),
                value = habits.peakHour?.let { hour ->
                    String.format(
                        Locale.getDefault(),
                        "%02d:00 · %s",
                        hour,
                        stringResource(peakPeriodOf(hour).labelRes)
                    )
                } ?: "—"
            )
            HabitRow(
                label = stringResource(R.string.stats_books_read),
                value = "${habits.distinctGalleries}"
            )
        }
    }
}

@Composable
private fun HabitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = stringResource(R.string.stats_empty_hint),
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}
