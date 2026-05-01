package io.tl.mynhentai.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.mynhentai.ui.components.MangaListItem
import io.tl.mynhentai.ui.components.RoundedDropdownMenu
import org.koin.androidx.compose.koinViewModel

private val sortOptions = listOf("date", "popular", "popular-week", "popular-today")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    var query by remember { mutableStateOf(initialQuery) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.search(initialQuery)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    Row {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Sort")
                            }
                            RoundedDropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                options = sortOptions,
                                selectedOption = currentSort,
                                onOptionSelected = { viewModel.setSort(it) }
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank()) viewModel.search(query)
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                )
            )

            if (currentSort != "date") {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.setSort("date") },
                    label = { Text(currentSort.replace("-", " "), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            if (uiState is SearchUiState.Idle && searchHistory.isNotEmpty() && query.isBlank()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    items(searchHistory, key = { it }) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = historyItem
                                    viewModel.search(historyItem)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = historyItem,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeHistoryItem(historyItem) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp)
                ) {
                    when (val state = uiState) {
                        is SearchUiState.Idle -> {
                            if (searchHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Enter a search query")
                                }
                            }
                        }

                        is SearchUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        is SearchUiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
