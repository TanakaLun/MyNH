package io.tl.mynhentai.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.ui.components.BlurredNavbar
import io.tl.mynhentai.ui.components.NavbarItem
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import io.tl.mynhentai.ui.detail.DetailScreen
import io.tl.mynhentai.ui.history.HistoryScreen
import io.tl.mynhentai.ui.home.HomeScreen
import io.tl.mynhentai.ui.library.LibraryScreen
import io.tl.mynhentai.ui.reader.ReaderScreen
import io.tl.mynhentai.ui.search.SearchResultsScreen
import io.tl.mynhentai.ui.search.SearchScreen
import io.tl.mynhentai.ui.settings.SettingsScreen
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.unit.dp

private val navbarItems = listOf(
    NavbarItem(R.string.nav_home, MiuixIcons.Home, Routes.HOME),
    NavbarItem(R.string.nav_history, MiuixIcons.Recent, Routes.HISTORY),
    NavbarItem(R.string.nav_favorites, MiuixIcons.Favorites, Routes.LIBRARY),
    NavbarItem(R.string.nav_settings, MiuixIcons.Settings, Routes.SETTINGS)
)

private val swipeDismiss = NavSwipeDirection.LeftToRight

@Composable
fun MainNavGraph(
    initialDeepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Main)
    val navigator = remember { Navigator(backStack) }

    val settings: SettingsHelper = koinInject()
    val backAnimStyle by settings.backAnimStyleFlow.collectAsState()

    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            val parts = initialDeepLink.split("/")
            if (parts.size >= 2 && parts[0] == "gallery") {
                val id = parts[1].toLongOrNull()
                if (id != null) {
                    navigator.push(AppRoute.Detail(id))
                }
            }
            onDeepLinkConsumed()
        }
    }

    val isCrossActivityStyle = backAnimStyle == "scale"
    val cornerRadius = rememberNavSystemCornerRadius()
    val isDark = isSystemInDarkTheme()
    val backdropColor = MiuixTheme.colorScheme.surface
    val effects = remember(backAnimStyle, cornerRadius, isDark, backdropColor) {
        when (backAnimStyle) {
            "none" -> NavDisplayEffects.None
            else -> NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = if (isCrossActivityStyle && cornerRadius == 0.dp) 32.dp else cornerRadius,
                cornerClipMode = if (isCrossActivityStyle) NavCornerClipMode.All else NavCornerClipMode.Leading,
                dimAmount = when {
                    isCrossActivityStyle -> if (isDark) 0.8f else 0.2f
                    else -> 0.5f
                },
                blockInputDuringTransition = true,
                backdropColor = backdropColor
            )
        }
    }
    val navTransition = when (backAnimStyle) {
        "scale" -> CrossActivityTransition
        "none" -> NavTransitions.None
        else -> NavTransitions.MiuixDefault
    }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        transition = navTransition,
        effects = effects,
        modifier = Modifier.fillMaxSize()
    ) {
        entry<AppRoute.Main> {
            MainTabs(navigator = navigator)
        }
        entry<AppRoute.Search>(swipeDismiss = swipeDismiss) { route ->
            SearchScreen(
                initialQuery = route.query,
                onBack = { navigator.pop() },
                onSearch = { q -> navigator.push(AppRoute.SearchResults(q)) }
            )
        }
        entry<AppRoute.SearchResults>(swipeDismiss = swipeDismiss) { route ->
            SearchResultsScreen(
                query = route.query,
                onBack = { navigator.pop() },
                onItemClick = { navigator.push(AppRoute.Detail(it)) }
            )
        }
        entry<AppRoute.Detail>(swipeDismiss = swipeDismiss) { route ->
            DetailScreen(
                galleryId = route.id,
                onBack = { navigator.pop() },
                onReaderClick = { navigator.push(AppRoute.Reader(it)) },
                onTagClick = { q -> navigator.push(AppRoute.Search(q)) }
            )
        }
        entry<AppRoute.Reader>(swipeDismiss = NavSwipeDirection.None) { route ->
            ReaderScreen(
                galleryId = route.id,
                onBack = { navigator.pop() }
            )
        }
    }
}

@Composable
private fun MainTabs(navigator: Navigator) {
    val settings: SettingsHelper = koinInject()
    val enableBlur by settings.enableBlurFlow.collectAsState()
    val useFloatingNavbar by settings.useFloatingNavbarFlow.collectAsState()
    val floatingNavbarStyle by settings.floatingNavbarStyleFlow.collectAsState()
    val floatingNavbarPosition by settings.floatingNavbarPositionFlow.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val backdrop = rememberBlurBackdrop(enableBlur = enableBlur)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            BlurredNavbar(
                items = navbarItems,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                backdrop = backdrop,
                useFloating = useFloatingNavbar,
                floatingStyle = floatingNavbarStyle,
                floatingPosition = floatingNavbarPosition,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        bottomNavPadding = innerPadding.calculateBottomPadding(),
                        onSearchClick = { navigator.push(AppRoute.Search()) },
                        onItemClick = { navigator.push(AppRoute.Detail(it)) }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        bottomNavPadding = innerPadding.calculateBottomPadding(),
                        onItemClick = { navigator.push(AppRoute.Detail(it)) }
                    )
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        bottomNavPadding = innerPadding.calculateBottomPadding(),
                        onItemClick = { navigator.push(AppRoute.Detail(it)) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(bottomNavPadding = innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}