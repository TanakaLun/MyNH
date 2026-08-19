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
        val items: List<MangaSummary>
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchResultsViewModel(
    private val repository: MangaRepository,
    private val settings: SettingsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _currentSort = MutableStateFlow("date")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    fun setSort(sort: String) {
        _currentSort.value = sort
        val state = _uiState.value
        if (state is SearchUiState.Success) {
            val query = _currentQuery.value
            if (query.isNotBlank()) search(query)
        }
    }

    private val _currentQuery = MutableStateFlow("")

    fun search(query: String) {
        if (query.isBlank()) return
        _currentQuery.value = query
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val sort = _currentSort.value
                val finalQuery = if (settings.languageFilterEnabled && settings.languageFilter.isNotBlank()) {
                    "$query language:${settings.languageFilter}"
                } else query
                val response = repository.search(finalQuery, 1, sort)
                _uiState.value = SearchUiState.Success(items = response.result)
                settings.addSearchHistoryItem(query)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }
}