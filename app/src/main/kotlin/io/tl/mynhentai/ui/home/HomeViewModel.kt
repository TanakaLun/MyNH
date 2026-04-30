package io.tl.mynhentai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        val currentPage: Int = 1
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: MangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPopular()
        viewModelScope.launch { repository.refreshCdn() }
    }

    fun loadPopular(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val response = repository.getPopular(page)
                _uiState.value = HomeUiState.Success(
                    items = response.result,
                    currentPage = page
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
