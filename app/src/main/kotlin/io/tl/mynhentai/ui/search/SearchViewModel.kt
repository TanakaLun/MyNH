package io.tl.mynhentai.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val items: List<MangaSummary>,
        val query: String,
        val currentPage: Int = 1
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val repository: MangaRepository,
    private val settings: SettingsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchHistory = MutableStateFlow(settings.getSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _currentSort = MutableStateFlow("date")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    fun setSort(sort: String) {
        _currentSort.value = sort
        val state = _uiState.value
        if (state is SearchUiState.Success) {
            search(state.query, 1)
        }
    }

    fun search(query: String, page: Int = 1) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val sort = _currentSort.value
                val finalQuery = if (settings.languageFilterEnabled && settings.languageFilter.isNotBlank()) {
                    "$query language:${settings.languageFilter}"
                } else query
                val response = repository.search(finalQuery, page, sort)
                _uiState.value = SearchUiState.Success(
                    items = response.result,
                    query = query,
                    currentPage = page
                )
                val history = _searchHistory.value.toMutableList()
                history.remove(query)
                history.add(0, query)
                if (history.size > 10) history.removeAt(history.lastIndex)
                _searchHistory.value = history
                settings.saveSearchHistory(history)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun removeHistoryItem(query: String) {
        val history = _searchHistory.value.filter { it != query }
        _searchHistory.value = history
        settings.saveSearchHistory(history)
    }
}
