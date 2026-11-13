package io.tl.mynhentai.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import io.tl.mynhentai.ui.statistics.components.ActivityTrailCard
import io.tl.mynhentai.ui.statistics.components.DailyBarChart
import io.tl.mynhentai.ui.statistics.components.HabitCard
import io.tl.mynhentai.ui.statistics.components.OverviewCard
import io.tl.mynhentai.ui.statistics.components.SearchPreferenceCard
import io.tl.mynhentai.ui.statistics.components.TasteProfileCard
import io.tl.mynhentai.ui.statistics.components.TopTagsCard
import io.tl.mynhentai.ui.statistics.components.YearHeatmap
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun StatisticsScreen(
    bottomNavPadding: Dp = 0.dp,
    onItemClick: (Long) -> Unit,
    onSearchClick: (String) -> Unit,
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = true,
                blurStyle = 1,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(R.string.statistics),
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .overscroll(rememberOverscrollEffect())
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .scrollEndHaptic(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + bottomNavPadding +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    Section(title = stringResource(R.string.stats_overview)) {
                        OverviewCard(
                            stats = state.stats,
                            modifier = Modifier.sectionCardModifier()
                        )
                    }
                }

                item {
                    Section(title = stringResource(R.string.stats_heatmap)) {
                        ChartCard {
                            YearHeatmap(daily = state.stats.daily)
                        }
                    }
                }

                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallTitle(
                                text = stringResource(R.string.stats_daily),
                                modifier = Modifier.weight(1f)
                            )
                            RangeSelector(
                                selected = state.rangeDays,
                                onSelect = viewModel::setRangeDays
                            )
                        }
                        ChartCard {
                            DailyBarChart(
                                daily = state.stats.daily,
                                rangeDays = state.rangeDays
                            )
                        }
                    }
                }

                item {
                    Section(title = stringResource(R.string.stats_top_tags)) {
                        TopTagsCard(
                            topTags = state.stats.topTags,
                            modifier = Modifier.sectionCardModifier()
                        )
                    }
                }

                item {
                    Section(title = stringResource(R.string.stats_taste_profile)) {
                        TasteProfileCard(
                            tagGroups = state.stats.tagGroups,
                            modifier = Modifier.sectionCardModifier()
                        )
                    }
                }

                item {
                    Section(title = stringResource(R.string.stats_habits)) {
                        HabitCard(
                            habits = state.stats.habits,
                            modifier = Modifier.sectionCardModifier()
                        )
                    }
                }

                if (state.recordQueries) {
                    item {
                        Section(title = stringResource(R.string.stats_search_preferences)) {
                            SearchPreferenceCard(
                                topSearches = state.stats.topSearches,
                                recentSearches = state.stats.recentSearches,
                                onSearchClick = onSearchClick,
                                modifier = Modifier.sectionCardModifier()
                            )
                        }
                    }
                }

                item {
                    Section(title = stringResource(R.string.stats_activity_trail)) {
                        ActivityTrailCard(
                            trail = state.stats.trail,
                            resolveCover = viewModel::resolveCover,
                            onItemClick = onItemClick,
                            onSearchClick = onSearchClick,
                            modifier = Modifier.sectionCardModifier()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        SmallTitle(text = title)
        content()
    }
}

@Composable
private fun ChartCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

private fun Modifier.sectionCardModifier(): Modifier =
    this.fillMaxWidth().padding(horizontal = 12.dp)

@Composable
private fun RangeSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(7, 30).forEach { days ->
            val active = days == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MiuixTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(days) }
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(
                        if (days == 7) R.string.stats_range_7 else R.string.stats_range_30
                    ),
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (active) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                )
            }
        }
    }
}
