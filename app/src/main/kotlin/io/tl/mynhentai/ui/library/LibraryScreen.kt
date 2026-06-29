package io.tl.mynhentai.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.SelectionToolbar
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onItemClick: (Long) -> Unit,
    onScroll: (Boolean) -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val listState = rememberLazyListState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val isAtTop = index == 0 && offset == 0
            if (isAtTop) {
                onScroll(false)
            } else {
                val scrollingDown = if (index != previousIndex) {
                    index > previousIndex
                } else {
                    offset > previousScrollOffset
                }
                onScroll(scrollingDown)
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "Selected ${selectedIds.size}"
                        else stringResource(R.string.favorites)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_favorites),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
