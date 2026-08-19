package io.tl.mynhentai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val languageOptions = listOf("", "chinese", "english", "japanese")
private val languageLabels = mapOf(
    "" to "All",
    "chinese" to "中文",
    "english" to "English",
    "japanese" to "日本語"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    bottomNavPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val concurrency by viewModel.concurrency.collectAsState()
    val languageFilter by viewModel.languageFilter.collectAsState()
    val languageFilterEnabled by viewModel.languageFilterEnabled.collectAsState()
    val blacklistedTags by viewModel.blacklistedTags.collectAsState()
    val coilCacheSize by viewModel.coilCacheSize.collectAsState()
    val offlineCacheSize by viewModel.offlineCacheSize.collectAsState()
    val backAnimStyle by viewModel.backAnimStyle.collectAsState()
    val monetEnabled by viewModel.monetEnabled.collectAsState()
    val enableBlur by viewModel.enableBlur.collectAsState()
    val useFloatingNavbar by viewModel.useFloatingNavbar.collectAsState()
    val floatingNavbarStyle by viewModel.floatingNavbarStyle.collectAsState()
    val floatingNavbarPosition by viewModel.floatingNavbarPosition.collectAsState()
    var showBlacklistDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.refreshCacheSizes() }

    fun Long.formatSize(): String {
        if (this <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (kotlin.math.log10(this.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        return String.format(
            java.util.Locale.getDefault(),
            "%.1f %s",
            this / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    if (showBlacklistDialog) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.blacklist_management),
            onDismissRequest = { showBlacklistDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (blacklistedTags.isEmpty()) {
                    Text(
                        stringResource(R.string.no_blacklisted_tags),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blacklistedTags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MiuixTheme.colorScheme.errorContainer)
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    tag.tagName,
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onErrorContainer
                                )
                                IconButton(
                                    onClick = { viewModel.removeBlacklistedTag(tag.tagId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        MiuixIcons.Close,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = MiuixTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    text = stringResource(R.string.close),
                    onClick = { showBlacklistDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = true,
                blurStyle = 1,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(R.string.settings),
                    defaultWindowInsetsPadding = false,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor
                )
            }
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .overscroll(rememberOverscrollEffect())
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .scrollEndHaptic(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + bottomNavPadding + 16.dp
            )
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.language_preference))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        OverlaySpinnerPreference(
                            title = stringResource(R.string.language_filter),
                            items = languageLabels.values.toList().map { label -> DropdownItem(text = label) },
                            selectedIndex = languageLabels.values.toList().indexOf(languageLabels[languageFilter] ?: "All"),
                            onSelectedIndexChange = { index ->
                                val selected = languageLabels.values.toList()[index]
                                val key = languageLabels.entries.find { it.value == selected }?.key ?: ""
                                viewModel.setLanguageFilter(key)
                            }
                        )
                        SwitchPreference(
                            checked = languageFilterEnabled,
                            onCheckedChange = { viewModel.setLanguageFilterEnabled(it) },
                            title = stringResource(R.string.sync_language_to_search)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.downloads))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        SliderPreference(
                            value = concurrency.toFloat(),
                            onValueChange = { viewModel.setConcurrency(it.toInt()) },
                            title = stringResource(R.string.max_concurrent_downloads),
                            valueText = "$concurrency",
                            valueRange = 1f..30f,
                            steps = 9
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.cache))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.clear_image_cache),
                            summary = coilCacheSize.formatSize(),
                            onClick = { viewModel.clearCoilCache() }
                        )
                        if (offlineCacheSize > 0L) {
                            ArrowPreference(
                                title = stringResource(R.string.clear_offline_cache),
                                summary = offlineCacheSize.formatSize(),
                                onClick = { viewModel.clearOfflineCache() }
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.blacklist))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.blacklist_management),
                            summary = "${blacklistedTags.size}",
                            onClick = { showBlacklistDialog = true }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.appearance))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        SwitchPreference(
                            checked = monetEnabled,
                            onCheckedChange = { viewModel.setMonetEnabled(it) },
                            title = stringResource(R.string.monet),
                            summary = stringResource(R.string.monet_summary)
                        )
                        SwitchPreference(
                            checked = enableBlur,
                            onCheckedChange = { viewModel.setEnableBlur(it) },
                            title = stringResource(R.string.enable_blur)
                        )
                    }
                }
            }

            item {
                val styleOptions = listOf(
                    stringResource(R.string.floating_navbar_style_default),
                    stringResource(R.string.floating_navbar_style_ios)
                )
                val positionOptions = listOf(
                    stringResource(R.string.floating_navbar_position_center),
                    stringResource(R.string.floating_navbar_position_start),
                    stringResource(R.string.floating_navbar_position_end)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.navigation))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.use_floating_navbar),
                            checked = useFloatingNavbar,
                            onCheckedChange = { viewModel.setUseFloatingNavbar(it) }
                        )
                        androidx.compose.animation.AnimatedVisibility(visible = useFloatingNavbar) {
                            Column {
                                OverlaySpinnerPreference(
                                    title = stringResource(R.string.floating_navbar_style),
                                    items = styleOptions.map { style -> DropdownItem(text = style) },
                                    selectedIndex = floatingNavbarStyle.coerceIn(0, styleOptions.size - 1),
                                    onSelectedIndexChange = { viewModel.setFloatingNavbarStyle(it) }
                                )
                                androidx.compose.animation.AnimatedVisibility(visible = floatingNavbarStyle == 0) {
                                    OverlaySpinnerPreference(
                                        title = stringResource(R.string.floating_navbar_position),
                                        items = positionOptions.map { position -> DropdownItem(text = position) },
                                        selectedIndex = floatingNavbarPosition.coerceIn(0, positionOptions.size - 1),
                                        onSelectedIndexChange = { viewModel.setFloatingNavbarPosition(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                val animNames = listOf(stringResource(R.string.back_anim_slide), stringResource(R.string.back_anim_scale), stringResource(R.string.back_anim_none))
                val animValues = listOf("slide", "scale", "none")
                val currentAnimIndex = when (backAnimStyle) {
                    "scale" -> 1
                    "none" -> 2
                    else -> 0
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.back_animation))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        OverlaySpinnerPreference(
                            title = stringResource(R.string.back_animation),
                            items = animNames.map { name -> DropdownItem(text = name) },
                            selectedIndex = currentAnimIndex,
                            onSelectedIndexChange = { index ->
                                viewModel.setBackAnimStyle(animValues[index])
                            }
                        )
                    }
                }
            }
        }
        }
    }
}