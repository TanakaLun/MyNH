package io.tl.mynhentai.ui.reader

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import io.tl.mynhentai.ui.components.PreloadPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReaderScreen(
    galleryId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(galleryId) {
        viewModel.load(galleryId)
    }

    val currentPage by remember {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isNotEmpty()) visible.first().index + 1 else 1
        }
    }

    val totalPages = (uiState as? ReaderUiState.Success)?.pages?.size ?: 0

    LaunchedEffect(galleryId, currentPage) {
        if (uiState is ReaderUiState.Success && totalPages > 0) {
            viewModel.saveProgress(galleryId, currentPage)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ReaderUiState.Success && state.initialPage > 1) {
            listState.scrollToItem(state.initialPage - 1)
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)

            WindowCompat.setDecorFitsSystemWindows(window, false)

            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        onDispose {
            window?.let {
                val controller = WindowCompat.getInsetsController(it, view)
                WindowCompat.setDecorFitsSystemWindows(it, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (showSaveDialog != null) {
        val pageNum = showSaveDialog!!
        AlertDialog(
            onDismissRequest = { showSaveDialog = null },
            title = { Text("Save Page $pageNum") },
            text = { Text("Save this page to your device gallery?") },
            confirmButton = {
                TextButton(onClick = {
                    val state = uiState
                    if (state is ReaderUiState.Success) {
                        val page = state.pages.find { it.number == pageNum }
                        if (page != null) {
                            saveImageToGallery(context, viewModel.resolveImageUrl(page.path), galleryId, pageNum)
                        }
                    }
                    showSaveDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .consumeWindowInsets(WindowInsets(0, 0, 0, 0))
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is ReaderUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(showControls) {
                            if (!showControls) {
                                detectTapGestures(
                                    onTap = { viewModel.toggleControls() },
                                    onLongPress = { showSaveDialog = currentPage }
                                )
                            }
                        },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(state.pages, key = { it.number }) { page ->
                        SubcomposeAsyncImage(
                            model = viewModel.resolveImageUrl(page.path),
                            contentDescription = "Page ${page.number}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black),
                            contentScale = ContentScale.FillWidth,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .size(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(showControls) {
                                if (showControls) {
                                    detectTapGestures(
                                        onTap = { viewModel.toggleControls() }
                                    )
                                }
                            }
                    )

                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { value ->
                            val target = value.toInt().coerceIn(1, state.pages.size)
                            listState.scrollToItem(target - 1)
                        },
                        valueRange = 1f..state.pages.size.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                            .padding(horizontal = 16.dp),
                        thumb = {
                            SliderDefaults.Thumb(
                                thumbSizeMultiplier = 0.5f
                            )
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
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

private fun saveImageToGallery(context: Context, imageUrl: String, galleryId: Long, pageNum: Int) {
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes() ?: return@launch
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@launch

            val filename = "NHentai_${galleryId}_p$pageNum.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NHentai")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES + "/NHentai"
                )
                dir.mkdirs()
                val file = java.io.File(dir, filename)
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also {
                    it.data = android.net.Uri.fromFile(file)
                    context.sendBroadcast(it)
                }
            }
        } catch (_: Exception) { }
    }
}
