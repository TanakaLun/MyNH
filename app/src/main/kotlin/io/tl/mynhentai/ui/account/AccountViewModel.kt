package io.tl.mynhentai.ui.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.AccountStore
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.local.SyncDao
import io.tl.mynhentai.data.model.AccountState
import io.tl.mynhentai.data.repository.AccountRepository
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.data.repository.SyncProgress
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AccountUiState(
    val account: AccountState? = null,
    val favoritesSyncEnabled: Boolean = true,
    val blacklistSyncEnabled: Boolean = true,
    val localFavorites: Int = 0,
    val pendingCount: Int = 0,
    val syncing: Boolean = false,
    val progress: SyncProgress? = null,
    val binding: Boolean = false,
    val mergePrompt: Boolean = false,
)

data class AccountEvent(
    @StringRes val messageRes: Int,
    val detail: String? = null,
    val id: Long = System.nanoTime(),
)

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val accountStore: AccountStore,
    private val settings: SettingsHelper,
    private val mangaRepository: MangaRepository,
    syncDao: SyncDao,
) : ViewModel() {

    private val _binding = MutableStateFlow(false)
    private val _mergePrompt = MutableStateFlow(false)

    private val _events = MutableStateFlow<AccountEvent?>(null)
    val events: StateFlow<AccountEvent?> = _events.asStateFlow()

    private val _openGallery = MutableStateFlow<Long?>(null)
    val openGallery: StateFlow<Long?> = _openGallery.asStateFlow()

    private val prefsFlow = combine(
        settings.favoritesSyncEnabledFlow,
        settings.blacklistSyncEnabledFlow,
    ) { favorites, blacklist -> favorites to blacklist }

    private val countsFlow = combine(
        mangaRepository.getAllFavorites(),
        syncDao.observeCount(),
    ) { favorites, pending -> favorites.size to pending }

    private val syncFlow = combine(
        accountRepository.syncing,
        accountRepository.progress,
    ) { syncing, progress -> syncing to progress }

    private val transientFlow = combine(_binding, _mergePrompt) { binding, merge -> binding to merge }

    private val localFlow = combine(countsFlow, syncFlow, transientFlow) { counts, sync, transient ->
        LocalSnapshot(
            favorites = counts.first,
            pending = counts.second,
            syncing = sync.first,
            progress = sync.second,
            binding = transient.first,
            mergePrompt = transient.second,
        )
    }

    val uiState: StateFlow<AccountUiState> = combine(
        accountStore.state,
        prefsFlow,
        localFlow,
    ) { account, prefs, local ->
        AccountUiState(
            account = account,
            favoritesSyncEnabled = prefs.first,
            blacklistSyncEnabled = prefs.second,
            localFavorites = local.favorites,
            pendingCount = local.pending,
            syncing = local.syncing,
            progress = local.progress,
            binding = local.binding,
            mergePrompt = local.mergePrompt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState(),
    )

    init {
        _mergePrompt.value = accountStore.isBound() && settings.accountMergePending
        if (accountStore.isBound()) {
            viewModelScope.launch { accountRepository.refreshProfile() }
        }
    }

    fun bind(rawKey: String) {
        if (_binding.value) return
        viewModelScope.launch {
            _binding.value = true
            val result = accountRepository.bind(rawKey)
            _binding.value = false
            result.fold(
                onSuccess = {
                    _events.value = AccountEvent(R.string.account_bound_success)
                    _mergePrompt.value = settings.accountMergePending
                    if (!settings.accountMergePending) {
                        syncNow(linkLocal = false)
                    }
                },
                onFailure = { error ->
                    _events.value = AccountEvent(bindErrorRes(error), error.message)
                }
            )
        }
    }

    fun confirmMerge(mergeLocal: Boolean) {
        _mergePrompt.value = false
        settings.accountMergePending = false
        syncNow(linkLocal = mergeLocal)
    }

    fun dismissMergePrompt() {
        _mergePrompt.value = false
    }

    fun syncNow(linkLocal: Boolean = false) {
        if (!linkLocal && accountRepository.mergePending) {
            _mergePrompt.value = true
            return
        }
        viewModelScope.launch {
            val result = accountRepository.syncAll(linkLocal)
            result.fold(
                onSuccess = { _events.value = AccountEvent(R.string.account_sync_success) },
                onFailure = { error ->
                    if (settings.accountMergePending && !linkLocal) {
                        _mergePrompt.value = true
                    } else {
                        _events.value = AccountEvent(syncErrorRes(error), error.message)
                    }
                }
            )
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            accountRepository.refreshProfile().onSuccess {
                _events.value = AccountEvent(R.string.account_profile_refreshed)
            }
        }
    }

    fun unbind() {
        accountRepository.unbind()
        _events.value = AccountEvent(R.string.account_unbound)
    }

    fun setFavoritesSyncEnabled(enabled: Boolean) {
        settings.favoritesSyncEnabled = enabled
        if (enabled && accountStore.isBound()) syncNow()
    }

    fun setBlacklistSyncEnabled(enabled: Boolean) {
        settings.blacklistSyncEnabled = enabled
        if (enabled && accountStore.isBound()) syncNow()
    }

    fun randomFavorite() {
        viewModelScope.launch {
            accountRepository.randomFavorite().fold(
                onSuccess = { id -> _openGallery.value = id },
                onFailure = { error ->
                    val res = when {
                        error is HttpException && error.code() == 404 -> R.string.account_random_empty
                        else -> R.string.account_random_failed
                    }
                    _events.value = AccountEvent(res, error.message)
                }
            )
        }
    }

    fun consumeOpenGallery() {
        _openGallery.value = null
    }

    fun dismissEvent() {
        _events.value = null
    }

    private fun bindErrorRes(error: Throwable): Int = when {
        error is HttpException && error.code() == 401 -> R.string.account_bind_invalid
        error is HttpException && error.code() == 429 -> R.string.account_rate_limited
        error is IOException -> R.string.account_network_error
        else -> R.string.account_bind_failed
    }

    private fun syncErrorRes(error: Throwable): Int = when {
        error is HttpException && error.code() == 401 -> R.string.account_invalid_key
        error is HttpException && error.code() == 429 -> R.string.account_rate_limited
        error is IOException -> R.string.account_network_error
        else -> R.string.account_sync_failed
    }

    private data class LocalSnapshot(
        val favorites: Int,
        val pending: Int,
        val syncing: Boolean,
        val progress: SyncProgress?,
        val binding: Boolean,
        val mergePrompt: Boolean,
    )
}
