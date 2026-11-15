package io.tl.mynhentai.data.local

import io.tl.mynhentai.data.model.AccountProfile
import io.tl.mynhentai.data.model.AccountState
import io.tl.mynhentai.data.model.ApiUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Holds the bound account. The API key lives in [SecretStore]; everything else is a small
 * cached profile so the UI can render without a network round trip.
 */
class AccountStore(
    private val settings: SettingsHelper,
    private val secretStore: SecretStore,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AccountState?> = _state.asStateFlow()

    fun apiKey(): String? = secretStore.readApiKey()

    /** Key used for outgoing requests; an already rejected key is not sent again until rebound. */
    fun apiKeyForRequests(): String? =
        if (settings.accountAuthInvalid) null else secretStore.readApiKey()

    fun isBound(): Boolean = _state.value != null && !apiKey().isNullOrBlank()

    fun stageApiKey(apiKey: String) {
        secretStore.saveApiKey(apiKey)
    }

    fun restoreApiKey(apiKey: String?) {
        if (apiKey.isNullOrBlank()) {
            secretStore.clear()
        } else {
            secretStore.saveApiKey(apiKey)
        }
    }

    fun completeBind(apiKey: String, user: ApiUser) {
        secretStore.saveApiKey(apiKey)
        val previous = readState()?.profile
        val boundAt = previous
            ?.takeIf { it.id == user.id && it.boundAt > 0L }
            ?.boundAt
            ?: System.currentTimeMillis()
        val profile = AccountProfile(
            id = user.id,
            username = user.username,
            slug = user.slug,
            avatarUrl = user.avatarUrl,
            about = user.about,
            favoriteTags = user.favoriteTags,
            roles = user.roles,
            boundAt = boundAt,
        )
        persistProfile(profile)
        settings.accountLastSyncAt = 0L
        settings.accountAuthInvalid = false
        _state.value = readState()
    }

    fun updateProfile(user: ApiUser) {
        val current = readState() ?: return
        persistProfile(
            current.profile.copy(
                id = user.id,
                username = user.username,
                slug = user.slug,
                avatarUrl = user.avatarUrl,
                about = user.about,
                favoriteTags = user.favoriteTags,
                roles = user.roles,
            )
        )
        _state.value = readState()
    }

    fun setLastSyncAt(timestamp: Long) {
        settings.accountLastSyncAt = timestamp
        _state.value = readState()
    }

    fun markAuthInvalid() {
        settings.accountAuthInvalid = true
        _state.value = readState()
    }

    fun clearAuthInvalid() {
        if (!settings.accountAuthInvalid) return
        settings.accountAuthInvalid = false
        _state.value = readState()
    }

    fun unbind() {
        secretStore.clear()
        settings.accountProfileJson = ""
        settings.accountLastSyncAt = 0L
        settings.accountAuthInvalid = false
        _state.value = null
    }

    private fun persistProfile(profile: AccountProfile) {
        settings.accountProfileJson = json.encodeToString(AccountProfile.serializer(), profile)
    }

    private fun readState(): AccountState? {
        val raw = settings.accountProfileJson
        if (raw.isBlank()) return null
        val profile = runCatching {
            json.decodeFromString(AccountProfile.serializer(), raw)
        }.getOrNull() ?: return null
        return AccountState(
            profile = profile,
            lastSyncAt = settings.accountLastSyncAt,
            authInvalid = settings.accountAuthInvalid,
        )
    }
}
