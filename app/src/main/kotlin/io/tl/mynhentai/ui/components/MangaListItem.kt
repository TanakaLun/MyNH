package io.tl.mynhentai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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

private val cardShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalFoundationApi::class)
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

    if (onLongClick != null) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onLongClick
                ),
            shape = cardShape
        ) {
            MangaListItemContent(manga, imageUrl, language, isSelected)
        }
    } else {
        ElevatedCard(
            onClick = onItemClick,
            modifier = modifier.fillMaxWidth(),
            shape = cardShape
        ) {
            MangaListItemContent(manga, imageUrl, language, isSelected)
        }
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
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
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pages_format, manga.numPages),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (language != null) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(language, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
