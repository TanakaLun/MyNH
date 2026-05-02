package io.tl.mynhentai.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.ReadProgressEntity
import io.tl.mynhentai.data.model.MangaPage
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Success(
        val title: String,
        val pages: List<MangaPage>,
        val initialPage: Int = 1
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

    private var saveJob: Job? = null

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val detail = repository.getDetail(id)
                val savedProgress = repository.getReadProgress(id)
                _uiState.value = ReaderUiState.Success(
                    title = detail.title.pretty ?: detail.title.english ?: "",
                    pages = detail.pages.sortedBy { it.number },
                    initialPage = savedProgress?.pageNumber?.coerceIn(1, detail.pages.size) ?: 1
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

    fun saveProgress(galleryId: Long, pageNumber: Int) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            repository.saveReadProgress(ReadProgressEntity(galleryId = galleryId, pageNumber = pageNumber))
        }
    }
}
