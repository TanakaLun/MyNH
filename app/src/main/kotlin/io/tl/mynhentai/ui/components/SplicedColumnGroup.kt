package io.tl.mynhentai.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (title.isNotEmpty()) {
            SectionTitle(title = title)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SplicedItem(
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    content: @Composable () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    val shape by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "shape"
    )

    val cornerShape = RoundedCornerShape(
        topStart = if (isFirst || isPressed) shape else 0.dp,
        topEnd = if (isFirst || isPressed) shape else 0.dp,
        bottomStart = if (isLast || isPressed) shape else 0.dp,
        bottomEnd = if (isLast || isPressed) shape else 0.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cornerShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .then(
                if (!isFirst) {
                    Modifier.padding(top = 2.dp)
                } else {
                    Modifier
                }
            )
            .zIndex(if (isPressed) 1f else 0f)
    ) {
        content()
    }
}
