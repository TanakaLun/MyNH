package io.tl.mynhentai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.padding

private val sortOptions = listOf("popular", "popular-today", "popular-week")

@Composable
private fun sortOptionLabel(option: String): String {
    return when (option) {
        "popular" -> stringResource(R.string.sort_popular)
        "popular-today" -> stringResource(R.string.sort_popular_today)
        "popular-week" -> stringResource(R.string.sort_popular_week)
        else -> option
    }
}

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    bottomNavPadding: Dp = 0.dp,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    val sortLabels = sortOptions.map { sortOptionLabel(it) }
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    val sortMenuEntries = remember(currentSort) {
        listOf(
            DropdownEntry(
                items = sortOptions.mapIndexed { index, option ->
                    DropdownItem(
                        text = sortLabels[index],
                        selected = option == currentSort,
                        onClick = { viewModel.setSort(option) }
                    )
                }
            )
        )
    }

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
                    title = stringResource(R.string.browse),
                    titleColor = MiuixTheme.colorScheme.onBackground,
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    actions = {
                        OverlayIconDropdownMenu(entries = sortMenuEntries) {
                            Icon(MiuixIcons.Sort, contentDescription = "Sort")
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(MiuixIcons.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InfiniteProgressIndicator()
                    }
                }

                is HomeUiState.Success -> {
                    val contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + bottomNavPadding + 80.dp
                    )
                    PullToRefresh(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        pullToRefreshState = pullToRefreshState,
                        topAppBarScrollBehavior = topAppBarScrollBehavior,
                        contentPadding = contentPadding
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .overscroll(rememberOverscrollEffect())
                                .scrollEndHaptic(),
                            contentPadding = contentPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.items, key = { it.id }) { manga ->
                                MangaListItem(
                                    manga = manga,
                                    imageUrl = viewModel.resolveThumbnailUrl(manga.thumbnail),
                                    onItemClick = { onItemClick(manga.id) }
                                )
                            }
                        }
                    }

                    FloatingToolbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomNavPadding)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.previousPage() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    MiuixIcons.ChevronBackward,
                                    contentDescription = stringResource(R.string.previous_page),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${state.currentPage} / ${state.numPages}",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = { viewModel.nextPage() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    MiuixIcons.ChevronForward,
                                    contentDescription = stringResource(R.string.next_page),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MiuixTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.retry() }
                            ) {
                                Icon(MiuixIcons.Refresh, contentDescription = null)
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}