package io.tl.mynhentai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
    val id: Long,
    val username: String,
    val slug: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val theme: String = "black",
    @SerialName("is_staff") val isStaff: Boolean = false,
    @SerialName("is_superuser") val isSuperuser: Boolean = false,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val about: String = "",
    @SerialName("favorite_tags") val favoriteTags: String = "",
    val email: String? = null,
)

/** Locally cached profile; the API key itself never lives here. */
@Serializable
data class AccountProfile(
    val id: Long,
    val username: String,
    val slug: String = "",
    val avatarUrl: String = "",
    val about: String = "",
    val favoriteTags: String = "",
    val roles: List<String> = emptyList(),
    val boundAt: Long = 0L,
)

data class AccountState(
    val profile: AccountProfile,
    val lastSyncAt: Long,
    val authInvalid: Boolean,
)

@Serializable
data class ApiFavoriteResponse(
    val favorited: Boolean,
    @SerialName("num_favorites") val numFavorites: Int? = null,
)

@Serializable
data class RandomFavoriteResponse(
    val id: Long? = null,
)

@Serializable
data class BlacklistedTagDto(
    val id: Long,
    val type: String = "",
    val name: String = "",
    val slug: String = "",
    val count: Int = 0,
)

@Serializable
data class BlacklistListResponse(
    val tags: List<BlacklistedTagDto> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class BlacklistUpdateRequest(
    val added: List<Long> = emptyList(),
    val removed: List<Long> = emptyList(),
)

@Serializable
data class BlacklistApiResponse(
    val success: Boolean = false,
    val count: Int = 0,
)
