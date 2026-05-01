package io.tl.mynhentai.ui.reader

import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
                    CircularProgressIndicator(color = Color.White)
                }
            }

            is ReaderUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.pages, key = { it.number }) { page ->
                        SubcomposeAsyncImage(
                            model = viewModel.resolveImageUrl(page.path),
                            contentDescription = "Page ${page.number}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .size(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        )
                    }
                }

                PreloadPages(
                    pages = state.pages,
                    currentPage = currentPage,
                    resolveImageUrl = viewModel::resolveImageUrl
                )

                if (showControls) {
                    TopAppBar(
                        title = { Text(state.title, fontSize = 16.sp) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
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

@Composable
private fun PreloadPages(
    pages: List<io.tl.mynhentai.data.model.MangaPage>,
    currentPage: Int,
    resolveImageUrl: (String) -> String
) {
    val context = LocalContext.current
    val imageLoader: ImageLoader = koinInject()
    LaunchedEffect(currentPage) {
        val preloadCount = 5
        val start = currentPage.coerceAtMost(pages.size - 1)
        val end = (currentPage + preloadCount).coerceAtMost(pages.size)
        for (i in start until end) {
            val url = resolveImageUrl(pages[i].path)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(CoilSize.ORIGINAL)
                .build()
            imageLoader.enqueue(request)
        }
    }
}
