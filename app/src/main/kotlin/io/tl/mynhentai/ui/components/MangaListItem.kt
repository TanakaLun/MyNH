package io.tl.mynhentai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.data.model.TagHelper
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val cardShape = RoundedCornerShape(8.dp)

@Composable
fun MangaListItem(
    manga: MangaSummary,
    imageUrl: String,
    onItemClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val language = TagHelper.getLanguage(manga.tagIds)

    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 8.dp,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp),
        onClick = onItemClick,
        onLongPress = onLongClick
    ) {
        MangaListItemContent(manga, imageUrl, language, isSelected)
    }
}

@Composable
private fun MangaListItemContent(
    manga: MangaSummary,
    imageUrl: String,
    language: String?,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = manga.englishTitle ?: manga.japaneseTitle,
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(
                        if (manga.thumbnailWidth > 0) manga.thumbnailWidth.toFloat() / manga.thumbnailHeight
                        else 0.7f
                    )
                    .clip(cardShape),
                contentScale = ContentScale.Crop
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(
                            if (manga.thumbnailWidth > 0) manga.thumbnailWidth.toFloat() / manga.thumbnailHeight
                            else 0.7f
                        )
                        .clip(cardShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MiuixIcons.Ok,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = manga.englishTitle ?: manga.japaneseTitle ?: stringResource(R.string.untitled),
                style = MiuixTheme.textStyles.main,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pages_format, manga.numPages),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                if (language != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = language,
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
