package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.data.local.PowerSaveModeTracker
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.ui.navigation.Routes
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ClockOutline
import top.yukonga.miuix.kmp.icon.glass.FavoriteOutline
import top.yukonga.miuix.kmp.icon.glass.HomeOutline
import top.yukonga.miuix.kmp.icon.glass.Report
import top.yukonga.miuix.kmp.icon.glass.SettingsOutline
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Surface effect selected in the settings. Values match [SettingsHelper]. */
object SurfaceEffect {
    const val NONE = SettingsHelper.SURFACE_EFFECT_NONE
    const val BLUR = SettingsHelper.SURFACE_EFFECT_BLUR
    const val GLASS = SettingsHelper.SURFACE_EFFECT_GLASS
}

val LocalSurfaceEffect = compositionLocalOf { SurfaceEffect.BLUR }

/**
 * Records the screen content so that blur/glass chrome can sample it. Returns null when the
 * selected effect is disabled, the runtime shader is missing or the device is in power save mode.
 */
@Composable
fun rememberSurfaceBackdrop(): LayerBackdrop? {
    val effect = LocalSurfaceEffect.current
    if (effect == SurfaceEffect.NONE) return null

    val powerSaveModeTracker: PowerSaveModeTracker = koinInject()
    val isPowerSaveMode by powerSaveModeTracker.isPowerSaveMode.collectAsState()
    if (isPowerSaveMode) return null

    if (!isRuntimeShaderSupported()) return null

    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/** Top app bar that follows the selected surface effect. */
@Composable
fun AppTopBar(
    title: String,
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior?,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    when (LocalSurfaceEffect.current) {
        SurfaceEffect.GLASS -> GlassTopAppBar(
            title = title,
            modifier = modifier,
            backdrop = backdrop,
            scrollBehavior = scrollBehavior,
            progressiveBlur = backdrop != null,
            defaultWindowInsetsPadding = false,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )

        SurfaceEffect.BLUR -> {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = true,
                blurStyle = 1,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = title,
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    bottomContent = bottomContent,
                )
            }
        }

        else -> TopAppBar(
            title = title,
            modifier = modifier,
            defaultWindowInsetsPadding = false,
            scrollBehavior = scrollBehavior,
            color = MiuixTheme.colorScheme.surface,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    }
}

/** Icon button that becomes a glass pill button when the glass surface effect is active. */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        GlassIconButton(onClick = onClick, modifier = modifier) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/** Bottom navigation bar that switches between the miuix blur and glass implementations. */
@Composable
fun AppNavigationBar(
    items: List<NavbarItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    backdrop: LayerBackdrop?,
    useFloating: Boolean,
    floatingPosition: Int,
    modifier: Modifier = Modifier,
) {
    if (LocalSurfaceEffect.current == SurfaceEffect.GLASS) {
        val navigationItems = items.map { item ->
            GlassNavigationItem(
                icon = glassNavIcon(item.route),
                label = stringResource(item.labelResId),
            )
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassNavigationBar(
                items = navigationItems,
                selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0),
                onSelect = { index -> items.getOrNull(index)?.let { onNavigate(it.route) } },
                backdrop = backdrop,
            )
        }
    } else {
        BlurredNavbar(
            items = items,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            backdrop = backdrop,
            useFloating = useFloating,
            floatingPosition = floatingPosition,
            modifier = modifier,
        )
    }
}

private fun glassNavIcon(route: String): ImageVector = when (route) {
    Routes.HOME -> MiuixIcons.Glass.HomeOutline
    Routes.HISTORY -> MiuixIcons.Glass.ClockOutline
    Routes.LIBRARY -> MiuixIcons.Glass.FavoriteOutline
    Routes.STATISTICS -> MiuixIcons.Glass.Report
    else -> MiuixIcons.Glass.SettingsOutline
}
