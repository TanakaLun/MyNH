package io.tl.mynhentai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.liquid.IosLiquidGlassNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix-style bottom navigation bar with the same blur behaviour as the miuix example app.
 *
 * When a [backdrop] is captured, the bar content behind it is blurred:
 *  - Standard [NavigationBar]: full-width blur with a `surface` 0.8 overlay.
 *  - [FloatingNavigationBar] (Default style): rounded blur pill with `surfaceContainer` 0.6
 *    overlay and a glass highlight edge.
 *  - iOS-like style: liquid-glass lens refraction pill.
 */
@Composable
fun BlurredNavbar(
    items: List<NavbarItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    backdrop: LayerBackdrop?,
    useFloating: Boolean,
    floatingStyle: Int,
    floatingPosition: Int,
    modifier: Modifier = Modifier,
) {
    val blurActive = backdrop != null

    if (useFloating) {
        if (floatingStyle == 1) {
            val navigationItems = items.map { item ->
                NavigationItem(
                    label = stringResource(item.labelResId),
                    icon = item.icon,
                )
            }
            IosLiquidGlassNavigationBar(
                items = navigationItems,
                selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0),
                onItemClick = { index -> onNavigate(items[index].route) },
                backdrop = backdrop,
                isBlurActive = blurActive,
                modifier = modifier,
            )
        } else {
            val floatingBarColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
            val floatingBarShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
            val isDark = isSystemInDarkTheme()
            val floatingHighlight = remember(isDark) {
                if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
            }
            Box(modifier = modifier) {
                FloatingNavigationBar(
                    modifier = if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = floatingBarShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(0.6f)),
                                ),
                            ),
                            highlight = floatingHighlight,
                        )
                    } else {
                        Modifier
                    },
                    color = floatingBarColor,
                    horizontalAlignment = floatingPosition.toNavbarAlignment(),
                ) {
                    items.forEachIndexed { index, item ->
                        FloatingNavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            icon = item.icon,
                            label = stringResource(item.labelResId),
                        )
                    }
                }
            }
        }
    } else {
        val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .then(
                    if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(barColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .then(modifier),
        ) {
            NavigationBar(color = barColor) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        icon = item.icon,
                        label = stringResource(item.labelResId),
                    )
                }
            }
        }
    }
}

private fun Int.toNavbarAlignment(): Alignment.Horizontal = when (this) {
    1 -> Alignment.Start
    2 -> Alignment.End
    else -> Alignment.CenterHorizontally
}