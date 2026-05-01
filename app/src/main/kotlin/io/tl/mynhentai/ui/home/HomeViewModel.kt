package io.tl.mynhentai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val items: List<MangaSummary>,
        val currentPage: Int = 1,
        val numPages: Int = 1
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: MangaRepository,
    private val settings: SettingsHelper
) : ViewModel() {

    private val _currentSort = MutableStateFlow("popular")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPage(1)
        viewModelScope.launch { repository.refreshCdn() }
    }

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    fun loadPage(page: Int, sort: String = _currentSort.value) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                _currentSort.value = sort
                val language = settings.languageFilter
                val response = if (language.isNotBlank()) {
                    repository.search(query = "language:$language", page = page)
                } else {
                    repository.getGalleries(page = page, sort = sort)
                }
                val blacklistedTagIds = repository.getAllBlacklistedTagIds()
                val filteredItems = if (blacklistedTagIds.isEmpty()) {
                    response.result
                } else {
                    response.result.filter { manga ->
                        manga.tagIds.none { it.toLong() in blacklistedTagIds }
                    }
                }
                _uiState.value = HomeUiState.Success(
                    items = filteredItems,
                    currentPage = page,
                    numPages = response.numPages
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setSort(sort: String) {
        loadPage(1, sort)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state is HomeUiState.Success && state.currentPage < state.numPages) {
            loadPage(state.currentPage + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state is HomeUiState.Success && state.currentPage > 1) {
            loadPage(state.currentPage - 1)
        }
    }
}
