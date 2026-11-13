package io.tl.mynhentai.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.ActivityItem
import io.tl.mynhentai.data.model.SearchTerm
import io.tl.mynhentai.data.model.UsageKind
import io.tl.mynhentai.ui.statistics.activityKindLabelRes
import io.tl.mynhentai.ui.statistics.formatClock
import io.tl.mynhentai.ui.statistics.formatDuration
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPreferenceCard(
    topSearches: List<SearchTerm>,
    recentSearches: List<ActivityItem>,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        if (topSearches.isEmpty() && recentSearches.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_no_searches),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column {
                if (topSearches.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topSearches.forEach { term ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MiuixTheme.colorScheme.secondaryContainer)
                                        .clickable { onSearchClick(term.query) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${term.query} ×${term.count}",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (recentSearches.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_recent_searches),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    recentSearches.forEach { item ->
                        val query = item.query
                        val clickAction: (() -> Unit)? = query
                            ?.takeIf { it.isNotBlank() }
                            ?.let { term -> { onSearchClick(term) } }
                        BasicComponent(
                            title = query.orEmpty(),
                            endActions = {
                                Text(
                                    text = formatClock(item.at),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            },
                            onClick = clickAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTrailCard(
    trail: List<ActivityItem>,
    resolveCover: (String) -> String,
    onItemClick: (Long) -> Unit,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        if (trail.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_no_activity),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column {
                trail.forEach { item ->
                    TrailItem(
                        item = item,
                        resolveCover = resolveCover,
                        onItemClick = onItemClick,
                        onSearchClick = onSearchClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailItem(
    item: ActivityItem,
    resolveCover: (String) -> String,
    onItemClick: (Long) -> Unit,
    onSearchClick: (String) -> Unit,
) {
    val query = item.query
    val kindLabel = stringResource(activityKindLabelRes(item.kind))

    val summary = buildString {
        append(kindLabel)
        append(" · ")
        append(formatClock(item.at))
        if (item.durationMillis > 0) {
            append(" · ")
            append(formatDuration(item.durationMillis))
        }
    }

    BasicComponent(
        title = if (item.kind == UsageKind.SEARCH) {
            "「${query.orEmpty()}」"
        } else {
            item.title.orEmpty()
        },
        summary = summary,
        startAction = { TrailLeading(item = item, resolveCover = resolveCover) },
        onClick = trailAction(item, onItemClick, onSearchClick),
    )
}

private fun trailAction(
    item: ActivityItem,
    onItemClick: (Long) -> Unit,
    onSearchClick: (String) -> Unit,
): (() -> Unit)? {
    if (item.kind == UsageKind.SEARCH) {
        val query = item.query ?: return null
        if (query.isBlank()) return null
        return { onSearchClick(query) }
    }
    val galleryId = item.galleryId ?: return null
    return { onItemClick(galleryId) }
}

@Composable
private fun TrailLeading(
    item: ActivityItem,
    resolveCover: (String) -> String,
) {
    val coverPath = item.coverPath
    if (coverPath != null) {
        AsyncImage(
            model = resolveCover(coverPath),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(36.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (item.kind) {
                    UsageKind.READ -> MiuixIcons.Album
                    UsageKind.BROWSE -> MiuixIcons.Recent
                    else -> MiuixIcons.Search
                },
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
