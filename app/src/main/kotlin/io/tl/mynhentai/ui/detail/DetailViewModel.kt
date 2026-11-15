package io.tl.mynhentai.ui.detail

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.R
import io.tl.mynhentai.data.api.CdnRepository
import io.tl.mynhentai.data.local.AccountStore
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.MangaSummary
import io.tl.mynhentai.data.model.Tag
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.data.repository.AccountRepository
import io.tl.mynhentai.data.tracker.UsageTracker
import io.tl.mynhentai.ui.components.DownloadManager
import io.tl.mynhentai.ui.components.DownloadService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val detail: MangaDetail,
        val isFavorite: Boolean = false
    ) : DetailUiState
    data class Error(val error: Throwable) : DetailUiState
}

data class DetailMessage(
    @StringRes val messageRes: Int,
    val detail: String? = null,
)

class DetailViewModel(
    private val repository: MangaRepository,
    private val cdnRepository: CdnRepository,
    private val usageTracker: UsageTracker,
    private val accountRepository: AccountRepository,
    accountStore: AccountStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _related = MutableStateFlow<List<MangaSummary>>(emptyList())
    val related: StateFlow<List<MangaSummary>> = _related.asStateFlow()

    private val _archiveError = MutableStateFlow<DetailMessage?>(null)
    val archiveError: StateFlow<DetailMessage?> = _archiveError.asStateFlow()

    val archiveAvailable: StateFlow<Boolean> = accountStore.state
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun load(id: Long) {
        _related.value = emptyList()
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val detail = repository.getDetail(id)
                var isFav = repository.isFavorite(id).first()
                val serverFav = detail.isFavorited
                if (serverFav != null &&
                    serverFav != isFav &&
                    accountRepository.shouldCalibrateFavorites() &&
                    !accountRepository.hasPendingFavorite(id)
                ) {
                    // Server state wins unless the user has an unsent change for this gallery.
                    if (serverFav) {
                        repository.addFavorite(detail.toFavoriteEntity())
                    } else {
                        repository.removeFavorite(id)
                    }
                    isFav = serverFav
                }
                _uiState.value = DetailUiState.Success(
                    detail = detail,
                    isFavorite = isFav
                )
                usageTracker.recordBrowse(detail)

                runCatching { repository.getRelated(id) }
                    .onSuccess { _related.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e)
            }
        }
    }

    fun toggleFavorite(detail: MangaDetail, currentState: Boolean) {
        val currentStateValue = _uiState.value
        if (currentStateValue !is DetailUiState.Success) return

        _uiState.value = currentStateValue.copy(isFavorite = !currentState)

        viewModelScope.launch {
            if (currentState) {
                repository.removeFavorite(detail.id)
                accountRepository.onFavoriteRemoved(detail.id)
            } else {
                repository.addFavorite(detail.toFavoriteEntity())
                accountRepository.onFavoriteAdded(detail.id)
            }
        }
    }

    fun startArchiveDownload(
        context: Context,
        detail: MangaDetail,
        filename: String,
        format: String,
    ) {
        viewModelScope.launch {
            accountRepository.downloadUrl(detail.id, format).fold(
                onSuccess = { url ->
                    DownloadService.startArchive(
                        context = context,
                        url = url,
                        galleryId = detail.id,
                        title = filename,
                        targetDir = DownloadManager.defaultDownloadPath,
                        format = format,
                    )
                },
                onFailure = { error ->
                    _archiveError.value = DetailMessage(archiveErrorRes(error), error.message)
                }
            )
        }
    }

    fun consumeArchiveError() {
        _archiveError.value = null
    }

    private fun archiveErrorRes(error: Throwable): Int = when {
        error is HttpException && error.code() == 401 -> R.string.account_invalid_key
        error is HttpException && error.code() == 429 -> R.string.account_rate_limited
        error is HttpException && error.code() == 503 -> R.string.download_feature_disabled
        error is IOException -> R.string.account_network_error
        else -> R.string.download_archive_failed
    }

    private fun MangaDetail.toFavoriteEntity(): FavoriteEntity =
        FavoriteEntity(
            id = id,
            title = title.pretty ?: title.english ?: "",
            thumbnail = cover.path,
            thumbnailWidth = cover.width,
            thumbnailHeight = cover.height,
            numPages = numPages
        )

    fun blacklistTag(tag: Tag) {
        viewModelScope.launch {
            repository.addBlacklistedTag(
                BlacklistedTagEntity(
                    tagId = tag.id,
                    tagName = tag.name
                )
            )
            accountRepository.onBlacklistAdded(tag.id)
        }
    }

    fun unblacklistTag(tagId: Long) {
        viewModelScope.launch {
            repository.removeBlacklistedTag(tagId)
            accountRepository.onBlacklistRemoved(tagId)
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
