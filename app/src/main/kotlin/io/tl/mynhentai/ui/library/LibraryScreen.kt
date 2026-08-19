package io.tl.mynhentai.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.SelectionToolbar
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LibraryScreen(
    onItemClick: (Long) -> Unit,
    bottomNavPadding: Dp = 0.dp,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val listState = rememberLazyListState()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

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
                    title = if (selectionMode) "Selected ${selectedIds.size}" else stringResource(R.string.favorites),
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor
                )
            }
        },
        floatingToolbar = {
            SelectionToolbar(
                visible = selectionMode,
                selectedCount = selectedIds.size,
                onConfirm = {
                    viewModel.removeFavoritesByIds(selectedIds.toList())
                    selectionMode = false
                    selectedIds = emptySet()
                },
                onCancel = {
                    selectionMode = false
                    selectedIds = emptySet()
                },
                bottomPadding = bottomNavPadding
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
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_favorites),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .overscroll(rememberOverscrollEffect())
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + bottomNavPadding + 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites, key = { it.id }) { fav ->
                        MangaListItem(
                            manga = MangaSummary(
                                id = fav.id,
                                mediaId = "",
                                englishTitle = fav.title,
                                thumbnail = fav.thumbnail,
                                thumbnailWidth = fav.thumbnailWidth,
                                thumbnailHeight = fav.thumbnailHeight,
                                numPages = fav.numPages
                            ),
                            imageUrl = viewModel.resolveThumbnailUrl(fav.thumbnail),
                            onItemClick = {
                                if (selectionMode) {
                                    selectedIds = if (fav.id in selectedIds) {
                                        selectedIds - fav.id
                                    } else {
                                        selectedIds + fav.id
                                    }
                                    if (selectedIds.isEmpty()) {
                                        selectionMode = false
                                    }
                                } else {
                                    onItemClick(fav.id)
                                }
                            },
                            onLongClick = {
                                selectionMode = true
                                selectedIds = setOf(fav.id)
                            },
                            isSelected = fav.id in selectedIds
                        )
                    }
                }
            }
        }
    }
}
