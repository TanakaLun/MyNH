package io.tl.mynhentai.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    galleryId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(galleryId) {
        viewModel.load(galleryId)
    }

    val currentPage by remember {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isNotEmpty()) visible.first().index + 1 else 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleControls() }
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...", color = Color.White)
                }
            }

            is ReaderUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.pages, key = { it.number }) { page ->
                        AsyncImage(
                            model = viewModel.resolveImageUrl(page.path),
                            contentDescription = "Page ${page.number}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                if (showControls) {
                    TopAppBar(
                        title = { Text(state.title) },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = onBack) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0x99000000),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    Text(
                        text = "Page $currentPage / ${state.pages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(
                                Color(0x99000000),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            is ReaderUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red
                    )
                }
            }
        }
    }
}
