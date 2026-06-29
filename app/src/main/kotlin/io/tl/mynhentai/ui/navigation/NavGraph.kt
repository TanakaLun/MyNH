package io.tl.mynhentai.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.detail.DetailScreen
import io.tl.mynhentai.ui.history.HistoryScreen
import io.tl.mynhentai.ui.home.HomeScreen
import io.tl.mynhentai.ui.library.LibraryScreen
import io.tl.mynhentai.ui.reader.ReaderScreen
import io.tl.mynhentai.ui.search.SearchScreen
import io.tl.mynhentai.ui.settings.SettingsScreen
import java.net.URLDecoder

data class BottomNavItem(val labelResId: Int, val icon: ImageVector, val route: String)

private val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    BottomNavItem(R.string.nav_history, Icons.Default.History, Routes.HISTORY),
    BottomNavItem(R.string.nav_favorites, Icons.Default.Bookmark, Routes.LIBRARY),
    BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Routes.SETTINGS)
)

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    var bottomBarHidden by remember { mutableStateOf(false) }
    val isMainTab = currentRoute in bottomNavItems.map { it.route }
    val isReader = currentRoute == Routes.READER

    val bottomPadding by animateDpAsState(
        targetValue = if (isMainTab && !bottomBarHidden) 80.dp else 0.dp,
        animationSpec = tween(300)
    )

    Scaffold { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = if (isReader) {
                    Modifier
                } else {
                    Modifier
                        .padding(innerPadding)
                        .padding(bottom = bottomPadding)
                }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onSearchClick = { navController.navigate(Routes.SEARCH) },
                        onItemClick = { id -> navController.navigate(Routes.detail(id)) },
                        onScroll = { hidden -> bottomBarHidden = hidden }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onItemClick = { id -> navController.navigate(Routes.detail(id)) },
                        onScroll = { hidden -> bottomBarHidden = hidden }
                    )
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onItemClick = { id -> navController.navigate(Routes.detail(id)) },
                        onScroll = { hidden -> bottomBarHidden = hidden }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onItemClick = { id -> navController.navigate(Routes.detail(id)) }
                    )
                }
                composable(
                    Routes.SEARCH_QUERY,
                    arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
                ) { entry ->
                    val q = entry.arguments?.getString("query")?.decodeQueryParam() ?: ""
                    SearchScreen(
                        initialQuery = q,
                        onBack = { navController.popBackStack() },
                        onItemClick = { id -> navController.navigate(Routes.detail(id)) }
                    )
                }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    DetailScreen(
                        galleryId = id,
                        onBack = { navController.popBackStack() },
                        onReaderClick = { rid -> navController.navigate(Routes.reader(rid)) },
                        onTagClick = { q -> navController.navigate(Routes.search(q)) }
                    )
                }
                composable(
                    Routes.READER,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    ReaderScreen(galleryId = id, onBack = { navController.popBackStack() })
                }
            }

            AnimatedVisibility(
                visible = isMainTab && !bottomBarHidden,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val sel = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = sel,
                            onClick = {
                                bottomBarHidden = false
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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

private fun String.decodeQueryParam(): String = URLDecoder.decode(this, "UTF-8")
