package io.tl.mynhentai.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.model.MangaPage
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Success(
        val title: String,
        val pages: List<MangaPage>,
        val currentPage: Int = 1
    ) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class ReaderViewModel(
    private val repository: MangaRepository,
    private val cdnRepository: io.tl.mynhentai.data.api.CdnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val detail = repository.getDetail(id)
                _uiState.value = ReaderUiState.Success(
                    title = detail.title.pretty ?: detail.title.english ?: "",
                    pages = detail.pages.sortedBy { it.number }
                )
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun resolveImageUrl(path: String): String = cdnRepository.resolveImageUrl(path)
}
