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

    private val _languageFilterEnabled = MutableStateFlow(settings.languageFilterEnabled)
    val languageFilterEnabled: StateFlow<Boolean> = _languageFilterEnabled.asStateFlow()

    private val _monetEnabled = MutableStateFlow(settings.monetEnabled)
    val monetEnabled: StateFlow<Boolean> = _monetEnabled.asStateFlow()

    private val _backAnimStyle = MutableStateFlow(settings.backAnimStyle)
    val backAnimStyle: StateFlow<String> = _backAnimStyle.asStateFlow()

    private val _enableBlur = MutableStateFlow(settings.enableBlur)
    val enableBlur: StateFlow<Boolean> = _enableBlur.asStateFlow()

    private val _useFloatingNavbar = MutableStateFlow(settings.useFloatingNavbar)
    val useFloatingNavbar: StateFlow<Boolean> = _useFloatingNavbar.asStateFlow()

    private val _floatingNavbarStyle = MutableStateFlow(settings.floatingNavbarStyle)
    val floatingNavbarStyle: StateFlow<Int> = _floatingNavbarStyle.asStateFlow()

    private val _floatingNavbarPosition = MutableStateFlow(settings.floatingNavbarPosition)
    val floatingNavbarPosition: StateFlow<Int> = _floatingNavbarPosition.asStateFlow()

    val blacklistedTags: StateFlow<List<BlacklistedTagEntity>> = repository.getAllBlacklistedTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonetEnabled(enabled: Boolean) {
        settings.monetEnabled = enabled
        _monetEnabled.value = enabled
    }

    fun setConcurrency(value: Int) {
        settings.maxConcurrency = value
        _concurrency.value = value
    }

    fun setLanguageFilter(value: String) {
        settings.languageFilter = value
        _languageFilter.value = value
    }

    fun setLanguageFilterEnabled(enabled: Boolean) {
        settings.languageFilterEnabled = enabled
        _languageFilterEnabled.value = enabled
    }

    fun setBackAnimStyle(style: String) {
        settings.backAnimStyle = style
        _backAnimStyle.value = style
    }

    fun setEnableBlur(enabled: Boolean) {
        settings.enableBlur = enabled
        _enableBlur.value = enabled
    }

    fun setUseFloatingNavbar(enabled: Boolean) {
        settings.useFloatingNavbar = enabled
        _useFloatingNavbar.value = enabled
    }

    fun setFloatingNavbarStyle(style: Int) {
        settings.floatingNavbarStyle = style
        _floatingNavbarStyle.value = style
    }

    fun setFloatingNavbarPosition(position: Int) {
        settings.floatingNavbarPosition = position
        _floatingNavbarPosition.value = position
    }

    fun removeBlacklistedTag(tagId: Long) {
        viewModelScope.launch {
            repository.removeBlacklistedTag(tagId)
        }
    }

    private val _coilCacheSize = MutableStateFlow(0L)
    val coilCacheSize: StateFlow<Long> = _coilCacheSize.asStateFlow()

    private val _offlineCacheSize = MutableStateFlow(0L)
    val offlineCacheSize: StateFlow<Long> = _offlineCacheSize.asStateFlow()

    fun clearCoilCache() {
        settings.clearCoilCache()
        _coilCacheSize.value = 0L
    }

    fun clearOfflineCache() {
        settings.clearOfflineCache()
        _offlineCacheSize.value = 0L
    }

    fun refreshCacheSizes() {
        _coilCacheSize.value = settings.coilCacheSize()
        _offlineCacheSize.value = settings.offlineCacheSize()
    }
}
