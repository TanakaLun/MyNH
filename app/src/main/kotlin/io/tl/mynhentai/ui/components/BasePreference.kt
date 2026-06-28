package io.tl.mynhentai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BasePreference(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    trailing: @Composable BoxScope.() -> Unit = {},
    onTapPosition: ((Float) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    val baseModifier = modifier
        .fillMaxWidth()
        .clickable(
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = if (onTapPosition != null) {
            baseModifier.pointerInput(onTapPosition) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.firstOrNull()?.let {
                            onTapPosition(it.position.x)
                        }
                    }
                }
            }
        } else baseModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Box(Modifier.alpha(alpha)) {
            trailing()
        }
    }
}
