package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Dropdown preference that becomes a glass popup when the glass surface effect is active. */
@Composable
fun AppDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassDropdownPreference(
            title = title,
            items = items,
            selectedIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            onSelectedIndexChange = onSelectedIndexChange,
            backdrop = backdrop,
            modifier = modifier,
            enabled = enabled,
            summary = summary,
        )
    } else {
        OverlaySpinnerPreference(
            title = title,
            items = items.map { DropdownItem(text = it) },
            selectedIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            enabled = enabled,
            summary = summary,
        )
    }
}

/** Bottom sheet that follows the selected surface effect. */
@Composable
fun AppBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    backdrop: Backdrop?,
    onDismissFinished: (() -> Unit)? = null,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassBottomSheet(
            show = show,
            title = title,
            onDismissRequest = onDismissRequest,
            backdrop = backdrop,
            onDismissFinished = onDismissFinished,
            startAction = startAction,
            endAction = endAction,
            content = content,
        )
    } else {
        OverlayBottomSheet(
            show = show,
            title = title,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            startAction = startAction,
            endAction = endAction,
            content = content,
        )
    }
}

/** Dialog that follows the selected surface effect. */
@Composable
fun AppDialog(
    show: Boolean,
    title: String?,
    onDismissRequest: () -> Unit,
    backdrop: Backdrop?,
    summary: String? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassDialog(
            onDismissRequest = onDismissRequest,
            show = show,
            backdrop = backdrop,
            title = title,
            summary = summary,
            onDismissFinished = onDismissFinished,
            content = content,
        )
    } else {
        OverlayDialog(
            show = show,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = content,
        )
    }
}

/** Dialog action that becomes a [GlassDialogButton] when the glass surface effect is active. */
@Composable
fun AppDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    destructive: Boolean = false,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassDialogButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            role = when {
                destructive -> GlassDialogButtonRole.Destructive
                primary -> GlassDialogButtonRole.Primary
                else -> GlassDialogButtonRole.Normal
            },
        ) {
            Text(text = text, style = MiuixTheme.textStyles.button)
        }
    } else if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            Text(text = text)
        }
    } else {
        TextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (destructive) {
                ButtonDefaults.textButtonColors(
                    color = MiuixTheme.colorScheme.errorContainer,
                    textColor = MiuixTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.textButtonColors()
            },
        )
    }
}

/** State shared between an app bar menu trigger and its root level glass popup. */
class AppMenuState internal constructor(val anchor: GlassPopupAnchor) {
    var expanded by mutableStateOf(false)
}

@Composable
fun rememberAppMenuState(): AppMenuState {
    val anchor = rememberGlassPopupAnchor()
    return remember(anchor) { AppMenuState(anchor) }
}

/**
 * Menu trigger for a top app bar. In glass mode the trigger is a glass icon button whose popup is
 * rendered by [AppMenuOverlay] at the page root.
 */
@Composable
fun AppMenuTrigger(
    state: AppMenuState,
    entries: List<DropdownEntry>,
    icon: ImageVector,
    contentDescription: String?,
    glassIcon: ImageVector = icon,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassIconButton(
            onClick = { state.expanded = true },
            modifier = Modifier.glassPopupAnchor(
                anchor = state.anchor,
                cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                floating = true,
            ),
        ) {
            Icon(
                imageVector = glassIcon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(28.dp)
                    .glassPopupAnchorContent(state.anchor),
            )
        }
    } else {
        OverlayIconDropdownMenu(entries = entries) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/** Root [BoxScope] overlay for [AppMenuTrigger]; no-op outside glass mode. */
@Composable
fun BoxScope.AppMenuOverlay(
    state: AppMenuState,
    entries: List<DropdownEntry>,
    icon: ImageVector,
    backdrop: Backdrop?,
) {
    if (LocalSurfaceEffect.current != SurfaceEffect.GLASS) return
    GlassIconDropdownMenu(
        show = state.expanded,
        onDismissRequest = { state.expanded = false },
        anchor = state.anchor,
        anchorContent = { Icon(imageVector = icon, contentDescription = null) },
        entries = entries,
        backdrop = backdrop,
        collapseOnSelection = true,
    )
}
