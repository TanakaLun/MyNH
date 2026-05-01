package io.tl.mynhentai.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import java.net.URLDecoder
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.tl.mynhentai.ui.detail.DetailScreen
import io.tl.mynhentai.ui.home.HomeScreen
import io.tl.mynhentai.ui.library.LibraryScreen
import io.tl.mynhentai.ui.reader.ReaderScreen
import io.tl.mynhentai.ui.search.SearchScreen
import io.tl.mynhentai.ui.settings.SettingsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, Routes.HOME),
    BottomNavItem("Favorites", Icons.Default.Bookmark, Routes.LIBRARY),
    BottomNavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
)

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
    var bottomBarHidden by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && !bottomBarHidden,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
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
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val bottomPadding = if (bottomBarHidden) 0.dp else innerPadding.calculateBottomPadding()
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = bottomPadding
            )
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSearchClick = {
                        navController.navigate(Routes.SEARCH)
                    },
                    onItemClick = { id ->
                        navController.navigate(Routes.detail(id))
                    },
                    onScroll = { hidden -> bottomBarHidden = hidden }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { id ->
                        navController.navigate(Routes.detail(id))
                    }
                )
            }

            composable(
                route = Routes.SEARCH_QUERY,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")?.decodeQueryParam() ?: ""
                SearchScreen(
                    initialQuery = query,
                    onBack = { navController.popBackStack() },
                    onItemClick = { id ->
                        navController.navigate(Routes.detail(id))
                    }
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                DetailScreen(
                    galleryId = id,
                    onBack = { navController.popBackStack() },
                    onReaderClick = { readerId ->
                        navController.navigate(Routes.reader(readerId))
                    },
                    onTagClick = { tagQuery ->
                        navController.navigate(Routes.search(tagQuery))
                    }
                )
            }

            composable(
                route = Routes.READER,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                ReaderScreen(
                    galleryId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onItemClick = { id ->
                        navController.navigate(Routes.detail(id))
                    },
                    onScroll = { hidden -> bottomBarHidden = hidden }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun String.decodeQueryParam(): String {
    return URLDecoder.decode(this, "UTF-8")
}