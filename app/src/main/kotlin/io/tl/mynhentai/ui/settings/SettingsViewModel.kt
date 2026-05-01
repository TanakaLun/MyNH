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

    private val _languageFilter = MutableStateFlow(settings.languageFilter)
    val languageFilter: StateFlow<String> = _languageFilter.asStateFlow()

    fun setConcurrency(value: Int) {
        settings.maxConcurrency = value
        _concurrency.value = value
    }

    fun setLanguageFilter(value: String) {
        settings.languageFilter = value
        _languageFilter.value = value
    }

    fun clearCache() {
        settings.clearCache()
    }
}
