package io.tl.mynhentai.data.repository

import io.tl.mynhentai.data.api.AccountService
import io.tl.mynhentai.data.local.AccountStore
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.local.MangaDao
import io.tl.mynhentai.data.local.PendingKind
import io.tl.mynhentai.data.local.PendingSyncEntity
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.local.SyncDao
import io.tl.mynhentai.data.model.ApiUser
import io.tl.mynhentai.data.model.BlacklistUpdateRequest
import io.tl.mynhentai.data.model.MangaSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

enum class SyncPhase {
    PUSH_FAVORITES,
    MERGE_FAVORITES,
    PULL_FAVORITES,
    PUSH_BLACKLIST,
    PULL_BLACKLIST,
    DONE,
}

data class SyncProgress(
    val phase: SyncPhase,
    val current: Int = 0,
    val total: Int = 0,
)

/**
 * Account binding and two-way synchronisation for favorites and the tag blacklist.
 *
 * The API key is validated once against `GET /user`, then attached to every request by the
 * auth interceptor. Sync is push-then-pull with the server as the source of truth; failed
 * changes stay in the pending queue and are retried on the next sync.
 */
class AccountRepository(
    private val service: AccountService,
    private val accountStore: AccountStore,
    private val settings: SettingsHelper,
    private val mangaDao: MangaDao,
    private val syncDao: SyncDao,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    @Volatile
    private var lastFavoriteCallAt = 0L

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _progress = MutableStateFlow<SyncProgress?>(null)
    val progress: StateFlow<SyncProgress?> = _progress.asStateFlow()

    val mergePending: Boolean
        get() = settings.accountMergePending

    // region binding

    suspend fun bind(rawKey: String): Result<ApiUser> {
        val key = rawKey.trim()
        if (key.isBlank() || key.any { it.isWhitespace() }) {
            return Result.failure(IllegalArgumentException("invalid api key"))
        }
        val previous = accountStore.apiKey()
        val wasBound = accountStore.isBound()
        accountStore.stageApiKey(key)
        return try {
            val user = service.getMe()
            accountStore.completeBind(key, user)
            settings.accountMergePending = !wasBound && mangaDao.getFavoritesOnce().isNotEmpty()
            Result.success(user)
        } catch (e: kotlinx.coroutines.CancellationException) {
            accountStore.restoreApiKey(previous)
            throw e
        } catch (t: Throwable) {
            accountStore.restoreApiKey(previous)
            Result.failure(t)
        }
    }

    suspend fun refreshProfile(): Result<ApiUser> = runCatching {
        val user = service.getMe()
        accountStore.updateProfile(user)
        user
    }.onFailure { handleAuthFailure(it) }

    fun unbind() {
        accountStore.unbind()
        settings.accountMergePending = false
        scope.launch { syncDao.clear() }
        _progress.value = null
    }

    fun scheduleStartupSync() {
        if (!accountStore.isBound()) return
        scope.launch { syncIfStale() }
    }

    /** Syncs only when the last successful sync is old enough, used when opening a screen. */
    suspend fun syncIfStale(): Result<Unit>? {
        if (!accountStore.isBound()) return null
        if (settings.accountMergePending) return null
        if (!settings.favoritesSyncEnabled && !settings.blacklistSyncEnabled) return null
        val state = accountStore.state.value ?: return null
        if (state.authInvalid) return null
        if (System.currentTimeMillis() - state.lastSyncAt <= SYNC_STALE_MS) return null
        return syncAll()
    }

    // endregion

    // region sync

    suspend fun syncAll(linkLocal: Boolean = false): Result<Unit> = runCatching {
        if (!accountStore.isBound()) error("account not bound")
        if (!linkLocal && settings.accountMergePending) error("local favorites are not merged yet")
        syncMutex.withLock {
            _syncing.value = true
            try {
                accountStore.clearAuthInvalid()
                if (settings.favoritesSyncEnabled) syncFavorites(linkLocal)
                if (settings.blacklistSyncEnabled) syncBlacklist()
                settings.accountMergePending = false
                accountStore.setLastSyncAt(System.currentTimeMillis())
                _progress.value = SyncProgress(SyncPhase.DONE)
            } finally {
                _syncing.value = false
                _progress.value = null
            }
        }
    }.onFailure { handleAuthFailure(it) }

    private suspend fun syncFavorites(linkLocal: Boolean) {
        if (linkLocal) {
            // First bind: push every local favorite that the server does not know about yet.
            val remoteIds = fetchAllFavorites().map { it.id }.toSet()
            val localOnly = mangaDao.getFavoritesOnce().filter { it.id !in remoteIds }
            _progress.value = SyncProgress(SyncPhase.MERGE_FAVORITES, 0, localOnly.size)
            localOnly.forEach { favorite ->
                syncDao.enqueue(PendingSyncEntity(kind = PendingKind.FAVORITE_ADD, targetId = favorite.id))
            }
        }

        pushFavoritePending()

        val remote = fetchAllFavorites()
        // Favorites added while the sync was running must not be dropped when the mirror updates.
        val keepIds = syncDao.getByKind(PendingKind.FAVORITE_ADD).map { it.targetId }.toSet()
        applyRemoteFavorites(remote, keepIds)
    }

    private suspend fun pushFavoritePending() {
        val adds = syncDao.getByKind(PendingKind.FAVORITE_ADD)
        val removes = syncDao.getByKind(PendingKind.FAVORITE_REMOVE)
        val total = adds.size + removes.size
        var processed = 0

        for (row in adds) {
            _progress.value = SyncProgress(SyncPhase.PUSH_FAVORITES, processed + 1, total)
            pushFavoriteRow(row, add = true)
            processed++
        }
        for (row in removes) {
            _progress.value = SyncProgress(SyncPhase.PUSH_FAVORITES, processed + 1, total)
            pushFavoriteRow(row, add = false)
            processed++
        }
    }

    private suspend fun pushFavoriteRow(row: PendingSyncEntity, add: Boolean) {
        try {
            favoriteCall { if (add) service.addFavorite(row.targetId) else service.removeFavorite(row.targetId) }
            syncDao.delete(row.id)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // The gallery is gone on the server side: the intent is already satisfied.
                syncDao.delete(row.id)
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchAllFavorites(): List<MangaSummary> {
        val first = favoriteCall { service.getFavorites(1) }
        val pageCount = first.numPages.coerceIn(1, MAX_PAGES)
        val result = first.result.toMutableList()
        _progress.value = SyncProgress(SyncPhase.PULL_FAVORITES, 1, pageCount)
        for (page in 2..pageCount) {
            val next = favoriteCall { service.getFavorites(page) }
            result += next.result
            _progress.value = SyncProgress(SyncPhase.PULL_FAVORITES, page, pageCount)
        }
        return result
    }

    private suspend fun applyRemoteFavorites(
        remote: List<MangaSummary>,
        keepIds: Set<Long>,
    ) {
        val localById = mangaDao.getFavoritesOnce().associateBy { it.id }
        val remoteIds = remote.map { it.id }.toSet()
        val entities = remote.map { summary -> summary.toFavorite(localById[summary.id]) }.toMutableList()
        keepIds.filter { it !in remoteIds }.forEach { id ->
            localById[id]?.let { entities += it }
        }

        if (entities.isEmpty()) {
            mangaDao.deleteAllFavorites()
        } else {
            mangaDao.insertFavorites(entities)
            mangaDao.deleteFavoritesNotIn(entities.map { it.id })
        }
    }

    private suspend fun syncBlacklist() {
        val adds = syncDao.getByKind(PendingKind.BLACKLIST_ADD)
        val removes = syncDao.getByKind(PendingKind.BLACKLIST_REMOVE)
        if (adds.isNotEmpty() || removes.isNotEmpty()) {
            _progress.value = SyncProgress(SyncPhase.PUSH_BLACKLIST)
            service.updateBlacklist(
                BlacklistUpdateRequest(
                    added = adds.map { it.targetId },
                    removed = removes.map { it.targetId },
                )
            )
            (adds + removes).forEach { syncDao.delete(it.id) }
        }

        _progress.value = SyncProgress(SyncPhase.PULL_BLACKLIST)
        val remote = service.getBlacklist()
        val localById = mangaDao.getBlacklistedTagsOnce().associateBy { it.tagId }
        val entities = remote.tags.map { tag ->
            BlacklistedTagEntity(
                tagId = tag.id,
                tagName = tag.name,
                addedAt = localById[tag.id]?.addedAt ?: System.currentTimeMillis(),
            )
        }
        if (entities.isEmpty()) {
            mangaDao.deleteAllBlacklistedTags()
        } else {
            mangaDao.insertBlacklistedTags(entities)
            mangaDao.deleteBlacklistedTagsNotIn(entities.map { it.tagId })
        }
    }

    // endregion

    // region incremental changes

    suspend fun onFavoriteAdded(id: Long) {
        if (!canSyncFavorites()) return
        syncDao.deleteByTarget(PendingKind.FAVORITE_REMOVE, id)
        syncDao.enqueue(PendingSyncEntity(kind = PendingKind.FAVORITE_ADD, targetId = id))
        pushFavoriteNow(id, add = true)
    }

    suspend fun onFavoriteRemoved(id: Long) {
        if (!canSyncFavorites()) return
        syncDao.deleteByTarget(PendingKind.FAVORITE_ADD, id)
        syncDao.enqueue(PendingSyncEntity(kind = PendingKind.FAVORITE_REMOVE, targetId = id))
        pushFavoriteNow(id, add = false)
    }

    suspend fun onBlacklistAdded(tagId: Long) {
        if (!canSyncBlacklist()) return
        syncDao.deleteByTarget(PendingKind.BLACKLIST_REMOVE, tagId)
        syncDao.enqueue(PendingSyncEntity(kind = PendingKind.BLACKLIST_ADD, targetId = tagId))
    }

    suspend fun onBlacklistRemoved(tagId: Long) {
        if (!canSyncBlacklist()) return
        syncDao.deleteByTarget(PendingKind.BLACKLIST_ADD, tagId)
        syncDao.enqueue(PendingSyncEntity(kind = PendingKind.BLACKLIST_REMOVE, targetId = tagId))
    }

    private suspend fun pushFavoriteNow(id: Long, add: Boolean) {
        runCatching {
            favoriteCall { if (add) service.addFavorite(id) else service.removeFavorite(id) }
            syncDao.deleteByTarget(
                if (add) PendingKind.FAVORITE_ADD else PendingKind.FAVORITE_REMOVE,
                id
            )
        }.onFailure { handleAuthFailure(it) }
    }

    // endregion

    suspend fun randomFavorite(): Result<Long> = runCatching {
        if (!accountStore.isBound()) error("account not bound")
        val response = service.getRandomFavorite()
        response.id?.takeIf { it > 0 } ?: error("empty favorites")
    }.onFailure { handleAuthFailure(it) }

    /** Asks the server for a short lived archive URL (cbz / zip). */
    suspend fun downloadUrl(galleryId: Long, format: String): Result<String> = runCatching {
        if (!accountStore.isBound()) error("account not bound")
        val response = service.getDownloadUrl(galleryId, format)
        response.url.takeIf { it.isNotBlank() } ?: error("empty download url")
    }.onFailure { handleAuthFailure(it) }

    /** True when server reported favorite state can safely be applied to the local mirror. */
    fun shouldCalibrateFavorites(): Boolean =
        accountStore.isBound() && settings.favoritesSyncEnabled && !settings.accountAuthInvalid

    suspend fun hasPendingFavorite(galleryId: Long): Boolean =
        syncDao.hasPendingFavorite(galleryId)

    private fun canSyncFavorites(): Boolean =
        accountStore.isBound() && settings.favoritesSyncEnabled && !settings.accountAuthInvalid

    private fun canSyncBlacklist(): Boolean =
        accountStore.isBound() && settings.blacklistSyncEnabled && !settings.accountAuthInvalid

    private suspend fun <T> favoriteCall(block: suspend () -> T): T {
        val wait = FAVORITE_CALL_INTERVAL_MS - (System.currentTimeMillis() - lastFavoriteCallAt)
        if (wait > 0) delay(wait)
        lastFavoriteCallAt = System.currentTimeMillis()
        return block()
    }

    private fun handleAuthFailure(t: Throwable) {
        if (t is HttpException && t.code() == 401) {
            accountStore.markAuthInvalid()
        }
    }

    private fun MangaSummary.toFavorite(existing: FavoriteEntity?): FavoriteEntity =
        FavoriteEntity(
            id = id,
            title = englishTitle ?: japaneseTitle ?: existing?.title ?: "",
            thumbnail = thumbnail,
            thumbnailWidth = thumbnailWidth,
            thumbnailHeight = thumbnailHeight,
            numPages = numPages,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
        )

    private companion object {
        const val FAVORITE_CALL_INTERVAL_MS = 4_300L
        const val SYNC_STALE_MS = 15 * 60_000L
        const val MAX_PAGES = 200
    }
}
