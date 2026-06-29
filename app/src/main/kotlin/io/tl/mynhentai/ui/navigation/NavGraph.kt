package io.tl.mynhentai.ui.navigation

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.ui.detail.DetailScreen
import io.tl.mynhentai.ui.history.HistoryScreen
import io.tl.mynhentai.ui.home.HomeScreen
import io.tl.mynhentai.ui.library.LibraryScreen
import io.tl.mynhentai.ui.reader.ReaderScreen
import io.tl.mynhentai.ui.search.SearchScreen
import io.tl.mynhentai.ui.settings.SettingsScreen
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject
import java.net.URLDecoder

data class BottomNavItem(
    val labelResId: Int,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    BottomNavItem(R.string.nav_history, Icons.Default.History, Routes.HISTORY),
    BottomNavItem(R.string.nav_favorites, Icons.Default.Bookmark, Routes.LIBRARY),
    BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Routes.SETTINGS)
)

private val mainRoutes = listOf(Routes.HOME, Routes.HISTORY, Routes.LIBRARY, Routes.SETTINGS)

enum class SubPage { NONE, SEARCH, SEARCH_QUERY, DETAIL, READER }

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute in mainRoutes
    var bottomBarHidden by remember { mutableStateOf(false) }
    val navBarVisible = showBottomBar && !bottomBarHidden

    val bottomPadding = if (navBarVisible) 80.dp else 0.dp

    val settings: SettingsHelper = koinInject()
    var backAnimStyle by remember { mutableStateOf(settings.backAnimStyle) }

    var currentPredictiveProgress by remember { mutableStateOf(0f) }
    var isPredictingBack by remember { mutableStateOf(false) }

    val isOnMainPage = currentRoute in mainRoutes
    val isReader = currentRoute == Routes.READER

    var subPage by remember { mutableStateOf(SubPage.NONE) }
    var subPageId by remember { mutableStateOf(0L) }
    var subPageQuery by remember { mutableStateOf("") }

    fun navigateToSubPage(newSubPage: SubPage, id: Long = 0L, query: String = "") {
        subPage = newSubPage
        subPageId = id
        subPageQuery = query
    }

    fun popSubPage() {
        subPage = SubPage.NONE
        subPageId = 0L
        subPageQuery = ""
    }

    if (subPage != SubPage.NONE && subPage != SubPage.READER && backAnimStyle != "none") {
        PredictiveBackHandler(enabled = true) { progressFlow ->
            isPredictingBack = true
            try {
                progressFlow.collect { backEvent ->
                    currentPredictiveProgress = backEvent.progress
                }
                popSubPage()
            } catch (_: CancellationException) {
            } finally {
                isPredictingBack = false
                currentPredictiveProgress = 0f
            }
        }
    }

    if (subPage != SubPage.NONE && backAnimStyle != "none") {
        BackHandler { popSubPage() }
    }

    val eased = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(currentPredictiveProgress)
    val isAnimating = isPredictingBack && currentPredictiveProgress > 0f

    Box(Modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(bottom = bottomPadding)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onSearchClick = { navigateToSubPage(SubPage.SEARCH) },
                            onItemClick = { id -> navigateToSubPage(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }

                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            onItemClick = { id -> navigateToSubPage(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }

                    composable(Routes.LIBRARY) {
                        LibraryScreen(
                            onItemClick = { id -> navigateToSubPage(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen()
                    }
                }

                if (subPage != SubPage.NONE) {
                    val subPageModifier = if (isAnimating && subPage != SubPage.READER) {
                        when (backAnimStyle) {
                            "scale" -> {
                                val sc = 1f - 0.25f * eased
                                val cornerRadius = if (sc < 0.98f) 16.dp else 0.dp
                                Modifier
                                    .graphicsLayer {
                                        scaleX = sc
                                        scaleY = sc
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .background(MaterialTheme.colorScheme.background)
                            }
                            "slide" -> {
                                val slideXDp = 300.dp * eased
                                Modifier
                                    .graphicsLayer { translationX = slideXDp.toPx() }
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                    .background(MaterialTheme.colorScheme.background)
                            }
                            else -> Modifier
                        }
                    } else Modifier

                    Box(modifier = subPageModifier.fillMaxSize()) {
                        when (subPage) {
                            SubPage.SEARCH -> SearchScreen(
                                onBack = { popSubPage() },
                                onItemClick = { id -> navigateToSubPage(SubPage.DETAIL, id) }
                            )
                            SubPage.SEARCH_QUERY -> SearchScreen(
                                initialQuery = subPageQuery,
                                onBack = { popSubPage() },
                                onItemClick = { id -> navigateToSubPage(SubPage.DETAIL, id) }
                            )
                            SubPage.DETAIL -> DetailScreen(
                                galleryId = subPageId,
                                onBack = { popSubPage() },
                                onReaderClick = { id -> navigateToSubPage(SubPage.READER, id) },
                                onTagClick = { query -> navigateToSubPage(SubPage.SEARCH_QUERY, query = query) }
                            )
                            SubPage.READER -> ReaderScreen(
                                galleryId = subPageId,
                                onBack = { popSubPage() }
                            )
                            SubPage.NONE -> {}
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = navBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                bottomBarHidden = false
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelResId)) }
                        )
                    }
                }
            }
        }
    }
}

private fun String.decodeQueryParam(): String {
    return URLDecoder.decode(this, "UTF-8")
}
