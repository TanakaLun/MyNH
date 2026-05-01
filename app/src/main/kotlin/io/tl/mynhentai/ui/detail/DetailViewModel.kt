package io.tl.mynhentai.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.api.CdnRepository
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.ui.components.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val detail: MangaDetail,
        val isFavorite: Boolean = false
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(
    private val repository: MangaRepository,
    private val cdnRepository: CdnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val detail = repository.getDetail(id)
                repository.isFavorite(id).collect { isFav ->
                    _uiState.value = DetailUiState.Success(
                        detail = detail,
                        isFavorite = isFav
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun toggleFavorite(detail: MangaDetail, currentState: Boolean) {
        viewModelScope.launch {
            if (currentState) {
                repository.removeFavorite(detail.id)
            } else {
                repository.addFavorite(
                    FavoriteEntity(
                        id = detail.id,
                        title = detail.title.pretty ?: detail.title.english ?: "",
                        thumbnail = detail.cover.path,
                        thumbnailWidth = detail.cover.width,
                        thumbnailHeight = detail.cover.height,
                        numPages = detail.numPages
                    )
                )
            }
        }
    }

    fun blacklistTag(tag: Tag) {
        viewModelScope.launch {
            repository.addBlacklistedTag(
                BlacklistedTagEntity(
                    tagId = tag.id,
                    tagName = tag.name
                )
            )
        }
    }

    fun unblacklistTag(tagId: Long) {
        viewModelScope.launch {
            repository.removeBlacklistedTag(tagId)
        }
    }

    fun resolveImageUrl(path: String): String = cdnRepository.resolveImageUrl(path)

    fun resolveThumbnailUrl(path: String): String = cdnRepository.resolveThumbnailUrl(path)

    fun startDownload(context: Context, detail: MangaDetail, filename: String, targetDir: String) {
        val pages = detail.pages.map { it.number to cdnRepository.resolveImageUrl(it.path) }
        DownloadService.startDownload(context, pages, detail.id, filename, targetDir)
    }

    fun startCache(context: Context, detail: MangaDetail) {
        val pages = detail.pages.map { it.number to cdnRepository.resolveImageUrl(it.path) }
        val title = detail.title.pretty ?: detail.title.english ?: "gallery_${detail.id}"
        DownloadService.startCache(context, pages, detail.id, title)
    }
}
