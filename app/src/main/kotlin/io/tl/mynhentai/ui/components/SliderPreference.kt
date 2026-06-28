package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SliderPreference(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..12f,
    steps: Int = 12,
) {
    BasePreference(
        title = label,
        description = "${value}dp",
        modifier = modifier,
        trailing = {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.width(160.dp)
            )
        }
    )
}
