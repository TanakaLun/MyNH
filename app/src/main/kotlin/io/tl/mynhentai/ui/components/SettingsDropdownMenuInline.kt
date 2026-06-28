package io.tl.mynhentai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onGloballyPositioned
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDropdownMenuInline(
    label: String,
    currentValue: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var tapOffsetX by remember { mutableFloatStateOf(0f) }
    var parentWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { parentWidth = it.size.width }
    ) {
        BasePreference(
            title = label,
            modifier = modifier,
            onClick = { expanded = true },
            onTapPosition = { tapOffsetX = it },
            trailing = {
                Box(Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .background(MaterialTheme.colorScheme.surface),
                offset = DpOffset(
                    x = with(density) {
                        val tapXDp = tapOffsetX.toDp()
                        val estimatedMenuWidth = 200.dp
                        val parentDp = parentWidth.toDp()
                        val rightEdge = tapXDp + estimatedMenuWidth + 8.dp
                        if (rightEdge > parentDp) {
                            (parentDp - estimatedMenuWidth - 8.dp).coerceAtLeast(4.dp)
                        } else tapXDp
                    },
                    y = 0.dp
                ),
            ) {
                options.forEach { option ->
                    val isSelected = currentValue == option
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelected(option); expanded = false },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            ),
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            }
        }
    }
}
