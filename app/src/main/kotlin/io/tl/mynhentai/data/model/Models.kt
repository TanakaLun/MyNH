package io.tl.mynhentai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val result: List<T>,
    @SerialName("num_pages") val numPages: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Long? = null
)

@Serializable
data class MangaSummary(
    val id: Long,
    @SerialName("media_id") val mediaId: String,
    @SerialName("english_title") val englishTitle: String? = null,
    @SerialName("japanese_title") val japaneseTitle: String? = null,
    val thumbnail: String,
    @SerialName("thumbnail_width") val thumbnailWidth: Int,
    @SerialName("thumbnail_height") val thumbnailHeight: Int,
    @SerialName("num_pages") val numPages: Int,
    @SerialName("tag_ids") val tagIds: List<Int> = emptyList()
)

@Serializable
data class MangaDetail(
    val id: Long,
    @SerialName("media_id") val mediaId: String,
    val title: Title,
    val cover: Cover,
    val pages: List<MangaPage>,
    val tags: List<Tag> = emptyList(),
    @SerialName("num_pages") val numPages: Int,
    @SerialName("num_favorites") val numFavorites: Int = 0
)

@Serializable
data class Title(
    val english: String? = null,
    val japanese: String? = null,
    val pretty: String? = null
)

@Serializable
data class Cover(
    val path: String,
    val width: Int,
    val height: Int
)

@Serializable
data class MangaPage(
    val number: Int,
    val path: String,
    val width: Int,
    val height: Int
)

@Serializable
data class Tag(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int? = null,
    val slug: String? = null,
    val url: String? = null
)

@Serializable
data class CdnResponse(
    val servers: List<String>
)
