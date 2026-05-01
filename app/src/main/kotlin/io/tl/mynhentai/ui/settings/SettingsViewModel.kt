package io.tl.mynhentai.ui.settings

import androidx.lifecycle.ViewModel
import io.tl.mynhentai.data.local.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settings: SettingsHelper
) : ViewModel() {

    private val _concurrency = MutableStateFlow(settings.maxConcurrency)
    val concurrency: StateFlow<Int> = _concurrency.asStateFlow()

    fun setConcurrency(value: Int) {
        settings.maxConcurrency = value
        _concurrency.value = value
    }

    fun clearCache() {
        settings.clearCache()
    }
}
