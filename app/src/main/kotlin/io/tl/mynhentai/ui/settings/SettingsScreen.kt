package io.tl.mynhentai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import io.tl.mynhentai.R
import io.tl.mynhentai.ui.components.BasePreference
import io.tl.mynhentai.ui.components.ConfigToggle
import io.tl.mynhentai.ui.components.SectionTitle
import io.tl.mynhentai.ui.components.SettingsDropdownMenuInline
import io.tl.mynhentai.ui.components.SliderPreference
import io.tl.mynhentai.ui.components.SplicedColumnGroup
import io.tl.mynhentai.ui.components.SplicedItem
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
    viewModel: SettingsViewModel = koinViewModel()
) {
    val concurrency by viewModel.concurrency.collectAsState()
    val languageFilter by viewModel.languageFilter.collectAsState()
    val languageFilterEnabled by viewModel.languageFilterEnabled.collectAsState()
    val blacklistedTags by viewModel.blacklistedTags.collectAsState()
    val coilCacheSize by viewModel.coilCacheSize.collectAsState()
    val offlineCacheSize by viewModel.offlineCacheSize.collectAsState()
    var showBlacklistDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SplicedColumnGroup(title = stringResource(R.string.language_preference)) {
                SplicedItem(isFirst = true) {
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
                SplicedItem(isLast = true) {
                    ConfigToggle(
                        label = stringResource(R.string.sync_language_to_search),
                        checked = languageFilterEnabled,
                        onCheckedChange = { viewModel.setLanguageFilterEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SplicedColumnGroup(title = stringResource(R.string.downloads)) {
                SplicedItem(isFirst = true, isLast = true) {
                    SliderPreference(
                        label = stringResource(R.string.max_concurrent_downloads),
                        value = concurrency,
                        onValueChange = { viewModel.setConcurrency(it) },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SplicedColumnGroup(title = stringResource(R.string.cache)) {
                SplicedItem(isFirst = true) {
                    BasePreference(
                        title = stringResource(R.string.clear_image_cache, coilCacheSize.formatSize()),
                        onClick = { viewModel.clearCoilCache() }
                    )
                }
                if (offlineCacheSize > 0L) {
                    SplicedItem(isLast = true) {
                        BasePreference(
                            title = stringResource(R.string.clear_offline_cache, offlineCacheSize.formatSize()),
                            onClick = { viewModel.clearOfflineCache() }
                        )
                    }
                } else {
                    SplicedItem(isLast = true) {}
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SplicedColumnGroup(title = stringResource(R.string.blacklist)) {
                SplicedItem(isFirst = true, isLast = true) {
                    BasePreference(
                        title = stringResource(R.string.blacklist_management_count, blacklistedTags.size),
                        onClick = { showBlacklistDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
