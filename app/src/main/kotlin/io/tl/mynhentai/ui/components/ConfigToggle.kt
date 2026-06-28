package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ConfigToggle(
    label: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasePreference(
        title = label,
        description = description,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onCheckedChange(it) },
            )
        }
    )
}
