package io.tl.mynhentai.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.DownloadDialog
import io.tl.mynhentai.ui.components.TagChip
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = true,
                blurStyle = 1,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(R.string.detail),
                    titleColor = MiuixTheme.colorScheme.onBackground,
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
        if (blacklistTag != null) {
            OverlayDialog(
                show = true,
                title = stringResource(R.string.blacklist_tag),
                summary = stringResource(R.string.blacklist_tag_confirm, blacklistTag?.name ?: ""),
                onDismissRequest = { blacklistTag = null }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.blacklist),
                        onClick = {
                            blacklistTag?.let { viewModel.blacklistTag(it) }
                            blacklistTag = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { blacklistTag = null },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InfiniteProgressIndicator()
                    }
                }

                is DetailUiState.Success -> {
                    val detail = state.detail
                    val shape = RoundedCornerShape(12.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .scrollEndHaptic(),
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
                                style = MiuixTheme.textStyles.title3
                            )

                            Text(
                                text = stringResource(R.string.pages_favorites_format, detail.numPages, detail.numFavorites),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Icon(MiuixIcons.Play, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.read))
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onReaderClick(detail.id) },
                                        onLongClick = { showDownloadDialog = true }
                                    )
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.toggleFavorite(detail, state.isFavorite)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (state.isFavorite) MiuixIcons.FavoritesFill
                                else MiuixIcons.Favorites,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.isFavorite) stringResource(R.string.favorited) else stringResource(R.string.favorite))
                        }
                    }

                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(12.dp)
                    ) {
                        Column(
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
                                            .background(MiuixTheme.colorScheme.tertiaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.replaceFirstChar { it.uppercase() },
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onTertiaryContainer
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
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pages_preview),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.primary
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MiuixTheme.colorScheme.error
                    )
                }
            }
        }
    }
    }
}
