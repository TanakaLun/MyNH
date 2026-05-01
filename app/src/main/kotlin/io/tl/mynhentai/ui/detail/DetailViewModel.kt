package io.tl.mynhentai.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.api.CdnRepository
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.ui.components.DownloadManager
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

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Int, val total: Int) : DownloadState
    data object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

class DetailViewModel(
    private val repository: MangaRepository,
    private val cdnRepository: CdnRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

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

    fun downloadGallery(detail: MangaDetail, filename: String, targetDir: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0, detail.pages.size)
            try {
                val pages = detail.pages.map { it.number to resolveImageUrl(it.path) }
                _downloadState.value = DownloadState.Downloading(0, pages.size)
                downloadManager.downloadAndSave(
                    pages = pages,
                    galleryId = detail.id,
                    galleryTitle = filename,
                    targetDir = targetDir
                )
                _downloadState.value = DownloadState.Success
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun cacheGallery(detail: MangaDetail) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0, detail.pages.size)
            try {
                val count = downloadManager.cacheForOffline(
                    pages = detail.pages.map { it.number to it.path },
                    galleryId = detail.id,
                    imageBaseUrl = cdnRepository::resolveImageUrl
                )
                _downloadState.value = DownloadState.Success
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Cache failed")
            }
        }
    }
}
