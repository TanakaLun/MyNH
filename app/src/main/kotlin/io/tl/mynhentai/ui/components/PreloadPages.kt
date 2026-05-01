package io.tl.mynhentai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import io.tl.mynhentai.data.model.MangaPage
import org.koin.compose.koinInject

@Composable
fun PreloadPages(
    pages: List<MangaPage>,
    currentPage: Int,
    resolveImageUrl: (String) -> String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
