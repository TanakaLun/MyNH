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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SearchScreen(
    initialQuery: String = "",
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val searchHistory by viewModel.searchHistory.collectAsState()
    var query by remember { mutableStateOf(initialQuery) }
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            onSearch(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = true,
                blurStyle = 1,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(R.string.search),
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                        }
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { q ->
                            if (q.isNotBlank()) {
                                onSearch(q)
                            }
                        },
                        expanded = true,
                        onExpandedChange = {},
                        label = stringResource(R.string.search_hint)
                    )
                }
                Text(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onBack
                        ),
                    style = MiuixTheme.textStyles.title4
                        .copy(fontWeight = FontWeight.Bold),
                    color = MiuixTheme.colorScheme.primary
                )
            }

            if (searchHistory.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(searchHistory, key = { it }) { historyItem ->
                        Card(
                            modifier = Modifier.padding(bottom = 4.dp),
                            insideMargin = PaddingValues(0.dp),
                            showIndication = true,
                            onClick = {
                                query = historyItem
                                onSearch(historyItem)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    MiuixIcons.Recent,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = historyItem,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.removeHistoryItem(historyItem) }) {
                                    Icon(
                                        MiuixIcons.Close,
                                        contentDescription = "Remove",
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.search_empty))
                }
            }
        }
        }
    }
}