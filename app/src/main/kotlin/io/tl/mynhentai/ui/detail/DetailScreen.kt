package io.tl.mynhentai.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.data.model.TagDictionary
import io.tl.mynhentai.ui.components.DownloadManager
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    galleryId: Long,
    onBack: () -> Unit,
    onReaderClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    viewModel: DetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var blacklistTag by remember { mutableStateOf<Tag?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(galleryId) {
        viewModel.load(galleryId)
    }

    if (blacklistTag != null) {
        AlertDialog(
            onDismissRequest = { blacklistTag = null },
            title = { Text("Blacklist Tag") },
            text = { Text("Are you sure you want to blacklist \"${blacklistTag?.name}\"? Galleries containing this tag will be hidden.") },
            confirmButton = {
                TextButton(onClick = {
                    blacklistTag?.let { viewModel.blacklistTag(it) }
                    blacklistTag = null
                }) {
                    Text("Blacklist")
                }
            },
            dismissButton = {
                TextButton(onClick = { blacklistTag = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDownloadDialog) {
        val state = uiState
        if (state is DetailUiState.Success) {
            DownloadDialog(
                detail = state.detail,
                onDismiss = { showDownloadDialog = false },
                onDownload = { filename, path ->
                    viewModel.startDownload(context, state.detail, filename, path)
                    showDownloadDialog = false
                },
                onCache = {
                    viewModel.startCache(context, state.detail)
                    showDownloadDialog = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is DetailUiState.Success -> {
                val detail = state.detail
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = viewModel.resolveThumbnailUrl(detail.cover.path),
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(
                                    if (detail.cover.width > 0) detail.cover.width.toFloat() / detail.cover.height
                                    else 0.7f
                                )
                                .clip(shape),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = detail.title.pretty ?: detail.title.english ?: "Untitled",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "${detail.numPages} pages · ${detail.numFavorites} favorites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .combinedClickable(
                                    onClick = { onReaderClick(detail.id) },
                                    onLongClick = { showDownloadDialog = true }
                                )
                                .background(MaterialTheme.colorScheme.primary)
                                .defaultMinSize(minHeight = 40.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Read",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.toggleFavorite(detail, state.isFavorite)
                            },
                            shape = shape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (state.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = null
                            )
                            Text(if (state.isFavorite) "Favorited" else "Favorite")
                        }
                    }

                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tagsByType = detail.tags.groupBy { it.type }
                            tagsByType.forEach { (type, tags) ->
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    tags.forEach { tag ->
                                        TagChip(
                                            tag = tag,
                                            onClick = {
                                                onTagClick("${tag.type}:${tag.name}")
                                            },
                                            onLongClick = {
                                                blacklistTag = tag
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pages Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                detail.pages.take(20).chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        pair.forEach { page ->
                                            AsyncImage(
                                                model = viewModel.resolveImageUrl(page.path),
                                                contentDescription = "Page ${page.number}",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(
                                                        if (page.width > 0) page.width.toFloat() / page.height
                                                        else 0.7f
                                                    )
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is DetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(tag: Tag, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = TagDictionary.getDisplayName(tag),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DownloadDialog(
    detail: io.tl.mynhentai.data.model.MangaDetail,
    onDismiss: () -> Unit,
    onDownload: (filename: String, path: String) -> Unit,
    onCache: () -> Unit
) {
    var filename by remember { mutableStateOf(detail.title.pretty ?: detail.title.english ?: "gallery_${detail.id}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载漫画") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "保存至: ${io.tl.mynhentai.ui.components.DownloadManager.defaultDownloadPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "下载将在后台进行，可通过通知查看进度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onDownload(filename, DownloadManager.defaultDownloadPath) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下载 (Zip)")
                    }
                    FilledTonalButton(
                        onClick = { onCache() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("缓存")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
