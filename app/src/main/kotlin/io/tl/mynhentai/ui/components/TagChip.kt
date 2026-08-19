package io.tl.mynhentai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.data.model.TagDictionary
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(tag: Tag, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = TagDictionary.getDisplayName(tag),
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSecondaryContainer
        )
    }
}
