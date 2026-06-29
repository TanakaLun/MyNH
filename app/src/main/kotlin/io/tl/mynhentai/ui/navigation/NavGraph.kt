package io.tl.mynhentai.ui.navigation

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

data class BottomNavItem(val labelResId: Int, val icon: ImageVector, val route: String)

private val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    BottomNavItem(R.string.nav_history, Icons.Default.History, Routes.HISTORY),
    BottomNavItem(R.string.nav_favorites, Icons.Default.Bookmark, Routes.LIBRARY),
    BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Routes.SETTINGS)
)

enum class SubPage { NONE, SEARCH, SEARCH_QUERY, DETAIL, READER }

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isOnMainPage = currentRoute in bottomNavItems.map { it.route }

    var bottomBarHidden by remember { mutableStateOf(false) }

    val bottomPadding by animateDpAsState(
        targetValue = if (isOnMainPage && !bottomBarHidden) 80.dp else 0.dp,
        animationSpec = tween(300)
    )

    val settings: SettingsHelper = koinInject()
    var backAnimStyle by remember { mutableStateOf(settings.backAnimStyle) }

    var subPage by remember { mutableStateOf(SubPage.NONE) }
    var subPageId by remember { mutableStateOf(0L) }
    var subPageQuery by remember { mutableStateOf("") }

    fun openSub(p: SubPage, id: Long = 0L, q: String = "") {
        subPage = p; subPageId = id; subPageQuery = q
    }
    fun closeSub() {
        subPage = SubPage.NONE; subPageId = 0L; subPageQuery = ""
    }

    val hasSubPage = subPage != SubPage.NONE

    var currentPredictiveProgress by remember { mutableFloatStateOf(0f) }
    var isPredictingBack by remember { mutableStateOf(false) }
    var predictiveTouchYPx by remember { mutableFloatStateOf(-1f) }

    if (hasSubPage && backAnimStyle != "none") {
        PredictiveBackHandler(enabled = true) { progressFlow ->
            isPredictingBack = true
            try {
                progressFlow.collect { backEvent ->
                    currentPredictiveProgress = backEvent.progress
                    if (Build.VERSION.SDK_INT >= 35) predictiveTouchYPx = backEvent.touchY
                }
                closeSub()
            } catch (_: CancellationException) {
            } finally {
                isPredictingBack = false
                currentPredictiveProgress = 0f
            }
        }
    }

    BackHandler(hasSubPage) { closeSub() }

    val isOnSubPage = hasSubPage

    Box(Modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(bottom = bottomPadding)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onSearchClick = { openSub(SubPage.SEARCH) },
                            onItemClick = { id -> openSub(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }
                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            onItemClick = { id -> openSub(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }
                    composable(Routes.LIBRARY) {
                        LibraryScreen(
                            onItemClick = { id -> openSub(SubPage.DETAIL, id) },
                            onScroll = { hidden -> bottomBarHidden = hidden }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen()
                    }
                }

                if (isOnSubPage) {
                    val eased = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(currentPredictiveProgress)
                    val animating = isPredictingBack && currentPredictiveProgress > 0f

                    val overlayModifier = if (animating) {
                        when (backAnimStyle) {
                            "scale" -> {
                                val sc = 1f - 0.25f * eased
                                val roundShape = RoundedCornerShape(if (sc < 0.98f) 16.dp else 0.dp)
                                Modifier
                                    .graphicsLayer {
                                        scaleX = sc
                                        scaleY = sc
                                        val ty = if (predictiveTouchYPx >= 0f)
                                            (predictiveTouchYPx / size.height).coerceIn(0.1f, 0.9f)
                                        else 0.5f
                                        transformOrigin = TransformOrigin(0.5f, ty)
                                    }
                                    .clip(roundShape)
                                    .background(MaterialTheme.colorScheme.background)
                            }
                            "slide" -> {
                                val sideClip = RoundedCornerShape(
                                    topStart = if (currentPredictiveProgress > 0f) 16.dp else 0.dp,
                                    bottomStart = if (currentPredictiveProgress > 0f) 16.dp else 0.dp
                                )
                                Modifier
                                    .graphicsLayer { translationX = size.width * 0.4f * eased }
                                    .clip(sideClip)
                                    .background(MaterialTheme.colorScheme.background)
                            }
                            else -> Modifier
                        }
                    } else Modifier

                    Surface(
                        modifier = overlayModifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        when (subPage) {
                            SubPage.SEARCH -> SearchScreen(
                                onBack = { closeSub() },
                                onItemClick = { id -> openSub(SubPage.DETAIL, id) }
                            )
                            SubPage.SEARCH_QUERY -> SearchScreen(
                                initialQuery = subPageQuery,
                                onBack = { closeSub() },
                                onItemClick = { id -> openSub(SubPage.DETAIL, id) }
                            )
                            SubPage.DETAIL -> DetailScreen(
                                galleryId = subPageId,
                                onBack = { closeSub() },
                                onReaderClick = { id -> openSub(SubPage.READER, id) },
                                onTagClick = { q -> openSub(SubPage.SEARCH_QUERY, q = q) }
                            )
                            SubPage.READER -> ReaderScreen(
                                galleryId = subPageId,
                                onBack = { closeSub() }
                            )
                            SubPage.NONE -> {}
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isOnMainPage && !bottomBarHidden && !hasSubPage,
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

private fun String.decodeQueryParam(): String = URLDecoder.decode(this, "UTF-8")
