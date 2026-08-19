package io.tl.mynhentai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

@Composable
fun MiuixDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayListPopup(
        show = expanded,
        popupModifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        ListPopupColumn {
            options.forEachIndexed { index, option ->
                DropdownImpl(
                    item = DropdownItem(text = option.replace("-", " ")),
                    optionSize = options.size,
                    isSelected = selectedOption == option,
                    index = index,
                    onSelectedIndexChange = {
                        onOptionSelected(option)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
