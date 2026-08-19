package io.tl.mynhentai.ui.search

import androidx.lifecycle.ViewModel
import io.tl.mynhentai.data.local.SettingsHelper
import kotlinx.coroutines.flow.StateFlow

class SearchViewModel(
    private val settings: SettingsHelper
) : ViewModel() {

    val searchHistory: StateFlow<List<String>> = settings.searchHistoryFlow

    fun removeHistoryItem(query: String) {
        settings.removeSearchHistoryItem(query)
    }
}