package io.tl.mynhentai.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: MangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    fun search(query: String, page: Int = 1) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val response = repository.search(query, page)
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
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun removeHistoryItem(query: String) {
        _searchHistory.value = _searchHistory.value.filter { it != query }
    }
}
