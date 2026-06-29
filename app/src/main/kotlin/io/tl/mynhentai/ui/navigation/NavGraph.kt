package io.tl.mynhentai.ui.navigation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import io.tl.mynhentai.ui.components.AnimatedNavbar
import io.tl.mynhentai.ui.components.NavbarItem
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

private val navbarItems = listOf(
    NavbarItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    NavbarItem(R.string.nav_history, Icons.Default.History, Routes.HISTORY),
    NavbarItem(R.string.nav_favorites, Icons.Default.Bookmark, Routes.LIBRARY),
    NavbarItem(R.string.nav_settings, Icons.Default.Settings, Routes.SETTINGS)
)

private val mainRoutes = setOf(Routes.HOME, Routes.HISTORY, Routes.LIBRARY, Routes.SETTINGS)

enum class SubPage { NONE, SEARCH, SEARCH_QUERY, DETAIL, READER }

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isOnMainPage = currentRoute in mainRoutes

    var bottomBarHidden by remember { mutableStateOf(false) }

    val settings: SettingsHelper = koinInject()
    val backAnimStyle by settings.backAnimStyleFlow.collectAsState()

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

    val bottomPadding by animateDpAsState(
        targetValue = if (isOnMainPage && !bottomBarHidden && !hasSubPage) 80.dp else 0.dp,
        animationSpec = tween(300)
    )

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

    val isAnimating = isPredictingBack && currentPredictiveProgress > 0f
    val showNavbar = isOnMainPage && !bottomBarHidden && !hasSubPage

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
                    SettingsScreen(
                        onScroll = { hidden -> bottomBarHidden = hidden }
                    )
                }
            }

            AnimatedNavbar(
                visible = showNavbar,
                items = navbarItems,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    bottomBarHidden = false
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (hasSubPage) {
                val eased = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(currentPredictiveProgress)

                val overlayModifier = if (isAnimating) {
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
                        else -> Modifier.background(MaterialTheme.colorScheme.background)
                    }
                } else Modifier.background(MaterialTheme.colorScheme.background)

                Box(
                    modifier = overlayModifier.fillMaxSize()
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
}

private fun String.decodeQueryParam(): String = URLDecoder.decode(this, "UTF-8")
