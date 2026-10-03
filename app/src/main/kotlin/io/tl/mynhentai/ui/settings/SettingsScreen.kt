package io.tl.mynhentai.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlinx.coroutines.launch
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.BackupData
import io.tl.mynhentai.data.local.PowerSaveModeTracker
import io.tl.mynhentai.ui.components.BlurredBar
import io.tl.mynhentai.ui.components.rememberBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
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
    val powerSaveModeTracker: PowerSaveModeTracker = koinInject()
    val isPowerSaveMode by powerSaveModeTracker.isPowerSaveMode.collectAsState()
    val useFloatingNavbar by viewModel.useFloatingNavbar.collectAsState()
    val floatingNavbarStyle by viewModel.floatingNavbarStyle.collectAsState()
    val floatingNavbarPosition by viewModel.floatingNavbarPosition.collectAsState()
    var showBlacklistDialog by remember { mutableStateOf(false) }

    var showExportSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<BackupData?>(null) }
    var exportSelection by remember { mutableStateOf(Triple(true, true, true)) }
    var exportFavorites by remember { mutableStateOf(true) }
    var exportHistory by remember { mutableStateOf(true) }
    var exportSettings by remember { mutableStateOf(true) }
    var importFavorites by remember(pendingImport) { mutableStateOf(pendingImport?.favorites != null) }
    var importHistory by remember(pendingImport) { mutableStateOf(pendingImport?.history != null) }
    var importSettings by remember(pendingImport) { mutableStateOf(pendingImport?.settings != null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun showBackupResult(titleRes: Int, reason: String?) {
        val message = if (reason.isNullOrBlank()) {
            context.getString(titleRes)
        } else {
            "${context.getString(titleRes)}: $reason"
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val (favorites, history, backupSettings) = exportSelection
            scope.launch {
                val result = viewModel.exportBackup(uri, favorites, history, backupSettings)
                if (result.isSuccess) {
                    showBackupResult(R.string.export_success, null)
                } else {
                    showBackupResult(R.string.export_failed, result.exceptionOrNull()?.message)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.parseBackup(uri).fold(
                    onSuccess = { data ->
                        if (data.favorites == null && data.history == null && data.settings == null) {
                            showBackupResult(R.string.backup_invalid, null)
                        } else {
                            pendingImport = data
                            showImportSheet = true
                        }
                    },
                    onFailure = { e ->
                        showBackupResult(R.string.backup_invalid, e.message)
                    }
                )
            }
        }
    }

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

    OverlayBottomSheet(
        show = showExportSheet,
        title = stringResource(R.string.export_items),
        onDismissRequest = { showExportSheet = false },
        startAction = {
            IconButton(onClick = { showExportSheet = false }) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        },
        endAction = {
            IconButton(
                onClick = {
                    exportSelection = Triple(exportFavorites, exportHistory, exportSettings)
                    showExportSheet = false
                    exportLauncher.launch("mynhentai-backup.json")
                },
                enabled = exportFavorites || exportHistory || exportSettings
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = stringResource(R.string.export_confirm),
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .scrollEndHaptic()
                .overScrollVertical()
        ) {
            item {
                SmallTitle(
                    text = stringResource(R.string.backup),
                    insideMargin = PaddingValues(16.dp, 8.dp)
                )
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer)
                ) {
                    SwitchPreference(
                        checked = exportFavorites,
                        onCheckedChange = { exportFavorites = it },
                        title = stringResource(R.string.favorites)
                    )
                    SwitchPreference(
                        checked = exportHistory,
                        onCheckedChange = { exportHistory = it },
                        title = stringResource(R.string.history)
                    )
                    SwitchPreference(
                        checked = exportSettings,
                        onCheckedChange = { exportSettings = it },
                        title = stringResource(R.string.settings)
                    )
                }
            }
            item {
                Spacer(
                    Modifier.padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                            WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
        }
    }

    pendingImport?.let { backup ->
        OverlayBottomSheet(
            show = showImportSheet,
            title = stringResource(R.string.import_items),
            onDismissRequest = { showImportSheet = false },
            onDismissFinished = { pendingImport = null },
            startAction = {
                IconButton(onClick = { showImportSheet = false }) {
                    Icon(
                        imageVector = MiuixIcons.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            },
            endAction = {
                IconButton(
                    onClick = {
                        scope.launch {
                            val result = viewModel.importBackup(
                                backup, importFavorites, importHistory, importSettings
                            )
                            showImportSheet = false
                            if (result.isSuccess) {
                                showBackupResult(R.string.import_success, null)
                            } else {
                                showBackupResult(R.string.import_failed, result.exceptionOrNull()?.message)
                            }
                        }
                    },
                    enabled = importFavorites || importHistory || importSettings
                ) {
                    Icon(
                        imageVector = MiuixIcons.Ok,
                        contentDescription = stringResource(R.string.import_confirm),
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollEndHaptic()
                    .overScrollVertical()
            ) {
                item {
                    SmallTitle(
                        text = stringResource(R.string.backup),
                        insideMargin = PaddingValues(16.dp, 8.dp)
                    )
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer)
                    ) {
                        SwitchPreference(
                            checked = importFavorites,
                            onCheckedChange = { importFavorites = it },
                            title = stringResource(R.string.favorites),
                            enabled = backup.favorites != null
                        )
                        SwitchPreference(
                            checked = importHistory,
                            onCheckedChange = { importHistory = it },
                            title = stringResource(R.string.history),
                            enabled = backup.history != null
                        )
                        SwitchPreference(
                            checked = importSettings,
                            onCheckedChange = { importSettings = it },
                            title = stringResource(R.string.settings),
                            enabled = backup.settings != null
                        )
                    }
                }
                item {
                    Spacer(
                        Modifier.padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(
                state = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomNavPadding)
            )
        },
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
                            endActions = {
                                Text(
                                    text = coilCacheSize.formatSize(),
                                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
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
                            endActions = {
                                Text(
                                    text = "${blacklistedTags.size}",
                                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
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
                            checked = enableBlur && !isPowerSaveMode,
                            onCheckedChange = { viewModel.setEnableBlur(it) },
                            title = stringResource(R.string.enable_blur),
                            summary = if (isPowerSaveMode) {
                                stringResource(R.string.blur_disabled_power_save)
                            } else {
                                null
                            },
                            enabled = !isPowerSaveMode
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTitle(text = stringResource(R.string.backup))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.export_data),
                            onClick = { showExportSheet = true }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.import_data),
                            onClick = { importLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                }
            }
        }
        }
    }
}