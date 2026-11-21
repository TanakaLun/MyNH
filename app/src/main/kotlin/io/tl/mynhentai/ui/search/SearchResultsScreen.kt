package io.tl.mynhentai.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.AppTopBar
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.NetworkErrorDialog
import io.tl.mynhentai.ui.components.AppIconButton
import io.tl.mynhentai.ui.components.AppMenuOverlay
import io.tl.mynhentai.ui.components.AppMenuTrigger
import io.tl.mynhentai.ui.components.rememberAppMenuState
import io.tl.mynhentai.ui.components.rememberSurfaceBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val sortOptions = listOf("date", "popular", "popular-week", "popular-today")

@Composable
private fun sortOptionLabel(option: String): String {
    return when (option) {
        "popular" -> stringResource(R.string.sort_popular)
        "popular-today" -> stringResource(R.string.sort_popular_today)
        "popular-week" -> stringResource(R.string.sort_popular_week)
        "date" -> stringResource(R.string.sort_date)
        else -> option
    }
}

@Composable
fun SearchResultsScreen(
    query: String,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: SearchResultsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val listState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberSurfaceBackdrop()

    LaunchedEffect(query) {
        viewModel.search(query)
    }

    val errorState = uiState as? SearchUiState.Error
    if (errorState != null) {
        NetworkErrorDialog(
            error = errorState.error,
            onRetry = { viewModel.search(query) },
            onExit = onBack,
            backdrop = backdrop,
        )
    }

    val sortLabels = sortOptions.map { sortOptionLabel(it) }
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

    val menuState = rememberAppMenuState()
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.search_results),
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    AppIconButton(
                        onClick = onBack,
                        icon = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.back)
                    )
                },
                actions = {
                    AppMenuTrigger(
                        state = menuState,
                        entries = sortMenuEntries,
                        icon = MiuixIcons.Filter,
                        contentDescription = stringResource(R.string.sort),
                    )
                }
            )
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
            when (val state = uiState) {
                is SearchUiState.Idle, is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InfiniteProgressIndicator()
                    }
                }

                is SearchUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            .scrollEndHaptic(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            start = 12.dp,
                            end = 12.dp,
                            bottom = innerPadding.calculateBottomPadding() + 8.dp
                        ),
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

                is SearchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
    AppMenuOverlay(
        state = menuState,
        entries = sortMenuEntries,
        icon = MiuixIcons.Filter,
        backdrop = backdrop,
    )
    }
}
