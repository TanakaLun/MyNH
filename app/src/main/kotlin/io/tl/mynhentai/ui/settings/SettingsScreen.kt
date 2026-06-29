package io.tl.mynhentai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BasePreference
import io.tl.mynhentai.ui.components.ConfigToggle
import io.tl.mynhentai.ui.components.SettingsDropdownMenuInline
import io.tl.mynhentai.ui.components.SliderPreference
import io.tl.mynhentai.ui.components.SplicedColumnGroup
import org.koin.androidx.compose.koinViewModel

private val languageOptions = listOf("", "chinese", "english", "japanese")
private val languageLabels = mapOf(
    "" to "All",
    "chinese" to "中文",
    "english" to "English",
    "japanese" to "日本語"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onScroll: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val concurrency by viewModel.concurrency.collectAsState()
    val languageFilter by viewModel.languageFilter.collectAsState()
    val languageFilterEnabled by viewModel.languageFilterEnabled.collectAsState()
    val blacklistedTags by viewModel.blacklistedTags.collectAsState()
    val coilCacheSize by viewModel.coilCacheSize.collectAsState()
    val offlineCacheSize by viewModel.offlineCacheSize.collectAsState()
    val backAnimStyle by viewModel.backAnimStyle.collectAsState()
    var showBlacklistDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val isAtTop = index == 0 && offset == 0
            if (isAtTop) {
                onScroll(false)
            } else {
                val scrollingDown = if (index != previousIndex) {
                    index > previousIndex
                } else {
                    offset > previousScrollOffset
                }
                onScroll(scrollingDown)
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

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
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(stringResource(R.string.blacklist_management)) },
            text = {
                if (blacklistedTags.isEmpty()) {
                    Text(
                        stringResource(R.string.no_blacklisted_tags),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    tag.tagName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                IconButton(
                                    onClick = { viewModel.removeBlacklistedTag(tag.tagId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlacklistDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .overscroll(rememberOverscrollEffect()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                SplicedColumnGroup(title = stringResource(R.string.language_preference)) {
                    item {
                        SettingsDropdownMenuInline(
                            label = stringResource(R.string.language_filter),
                            currentValue = languageLabels[languageFilter] ?: "All",
                            options = languageLabels.values.toList(),
                            onSelected = { selectedLabel ->
                                val key = languageLabels.entries.find { it.value == selectedLabel }?.key ?: ""
                                viewModel.setLanguageFilter(key)
                            }
                        )
                    }
                    item {
                        ConfigToggle(
                            label = stringResource(R.string.sync_language_to_search),
                            checked = languageFilterEnabled,
                            onCheckedChange = { viewModel.setLanguageFilterEnabled(it) }
                        )
                    }
                }
            }

            item {
                SplicedColumnGroup(title = stringResource(R.string.downloads)) {
                    item {
                        SliderPreference(
                            label = stringResource(R.string.max_concurrent_downloads),
                            value = concurrency,
                            onValueChange = { viewModel.setConcurrency(it) },
                            valueRange = 1f..30f,
                            steps = 9
                        )
                    }
                }
            }

            item {
                SplicedColumnGroup(title = stringResource(R.string.cache)) {
                    item {
                        BasePreference(
                            title = stringResource(R.string.clear_image_cache),
                            onClick = { viewModel.clearCoilCache() },
                            trailing = {
                                Text(
                                    text = coilCacheSize.formatSize(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    if (offlineCacheSize > 0L) {
                        item {
                            BasePreference(
                                title = stringResource(R.string.clear_offline_cache),
                                onClick = { viewModel.clearOfflineCache() },
                                trailing = {
                                    Text(
                                        text = offlineCacheSize.formatSize(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                SplicedColumnGroup(title = stringResource(R.string.blacklist)) {
                    item {
                        BasePreference(
                            title = stringResource(R.string.blacklist_management),
                            onClick = { showBlacklistDialog = true },
                            trailing = {
                                Text(
                                    text = "${blacklistedTags.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }

            item {
                val animNames = listOf(stringResource(R.string.back_anim_slide), stringResource(R.string.back_anim_scale), stringResource(R.string.back_anim_none))
                val animValues = listOf("slide", "scale", "none")
                val currentAnimName = when (backAnimStyle) {
                    "scale" -> stringResource(R.string.back_anim_scale)
                    "none" -> stringResource(R.string.back_anim_none)
                    else -> stringResource(R.string.back_anim_slide)
                }
                SplicedColumnGroup(title = stringResource(R.string.back_animation)) {
                    item {
                        SettingsDropdownMenuInline(
                            label = stringResource(R.string.back_animation),
                            currentValue = currentAnimName,
                            options = animNames,
                            onSelected = { selected ->
                                val value = animValues[animNames.indexOf(selected)]
                                viewModel.setBackAnimStyle(value)
                            }
                        )
                    }
                }
            }
        }
    }
}
