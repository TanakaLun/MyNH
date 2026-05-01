package io.tl.mynhentai.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.RoundedDropdownMenu
import org.koin.androidx.compose.koinViewModel

private val sortOptions = listOf("popular", "popular-today", "popular-week")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onScroll: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp,
                        top = 72.dp,
                        bottom = 144.dp
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

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.previousPage() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous page"
                        )
                    }

                    Text(
                        text = "${state.currentPage} / ${state.numPages}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    SmallFloatingActionButton(
                        onClick = { viewModel.nextPage() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next page"
                        )
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.retry() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("重试")
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = { Text("Browse", fontSize = 18.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showSortMenu = true },
                        label = {
                            Text(
                                currentSort.replace("-", " "),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    RoundedDropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        options = sortOptions,
                        selectedOption = currentSort,
                        onOptionSelected = { viewModel.setSort(it) }
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )
    }
}
