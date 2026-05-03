package io.tl.mynhentai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoundedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    menuShape: androidx.compose.foundation.shape.CornerBasedShape = RoundedCornerShape(16.dp),
    itemShape: androidx.compose.foundation.shape.CornerBasedShape = RoundedCornerShape(12.dp),
    itemHorizontalPadding: Dp = 8.dp
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = menuShape)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.clip(menuShape)
        ) {
            Spacer(modifier = Modifier.height(3.dp))
            options.forEach { option ->
                val isSelected = selectedOption == option
                DropdownMenuItem(
                    text = {
                        Text(
                            option.replace("-", " "),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .padding(horizontal = itemHorizontalPadding, vertical = 2.dp)
                        .clip(itemShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    colors = MenuDefaults.itemColors(
                        textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}
