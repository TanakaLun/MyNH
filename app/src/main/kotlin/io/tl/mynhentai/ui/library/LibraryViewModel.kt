package io.tl.mynhentai.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.AccountStore
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.repository.AccountRepository
import io.tl.mynhentai.data.repository.MangaRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LibraryViewModel(
    private val repository: MangaRepository,
    private val accountRepository: AccountRepository,
    accountStore: AccountStore,
) : ViewModel() {

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    val favorites: StateFlow<List<FavoriteEntity>> = repository.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val bound: StateFlow<Boolean> = accountStore.state
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val syncing: StateFlow<Boolean> = accountRepository.syncing

    private val _syncMessage = MutableStateFlow<Int?>(null)
    val syncMessage: StateFlow<Int?> = _syncMessage.asStateFlow()

    private val _randomGallery = MutableStateFlow<Long?>(null)
    val randomGallery: StateFlow<Long?> = _randomGallery.asStateFlow()

    fun syncIfStale() {
        viewModelScope.launch {
            accountRepository.syncIfStale()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            accountRepository.syncAll().fold(
                onSuccess = { _syncMessage.value = R.string.account_sync_success },
                onFailure = { error -> _syncMessage.value = syncErrorRes(error) }
            )
        }
    }

    fun randomFavorite() {
        viewModelScope.launch {
            accountRepository.randomFavorite().fold(
                onSuccess = { _randomGallery.value = it },
                onFailure = { error ->
                    _syncMessage.value = if (error is HttpException && error.code() == 404) {
                        R.string.account_random_empty
                    } else {
                        R.string.account_random_failed
                    }
                }
            )
        }
    }

    fun consumeRandomGallery() {
        _randomGallery.value = null
    }

    fun consumeSyncMessage() {
        _syncMessage.value = null
    }

    fun removeFavoritesByIds(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.removeFavorite(id)
                accountRepository.onFavoriteRemoved(id)
            }
        }
    }

    private fun syncErrorRes(error: Throwable): Int = when {
        error is HttpException && error.code() == 401 -> R.string.account_invalid_key
        error is HttpException && error.code() == 429 -> R.string.account_rate_limited
        error is IOException -> R.string.account_network_error
        else -> R.string.account_sync_failed
    }
}
