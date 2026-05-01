package io.tl.mynhentai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsHelper,
    private val repository: MangaRepository
) : ViewModel() {

    private val _concurrency = MutableStateFlow(settings.maxConcurrency)
    val concurrency: StateFlow<Int> = _concurrency.asStateFlow()

    private val _languageFilter = MutableStateFlow(settings.languageFilter)
    val languageFilter: StateFlow<String> = _languageFilter.asStateFlow()

    val blacklistedTags: StateFlow<List<BlacklistedTagEntity>> = repository.getAllBlacklistedTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setConcurrency(value: Int) {
        settings.maxConcurrency = value
        _concurrency.value = value
    }

    fun setLanguageFilter(value: String) {
        settings.languageFilter = value
        _languageFilter.value = value
    }

    fun removeBlacklistedTag(tagId: Long) {
        viewModelScope.launch {
            repository.removeBlacklistedTag(tagId)
        }
    }

    fun clearCache() {
        settings.clearCache()
    }
}
