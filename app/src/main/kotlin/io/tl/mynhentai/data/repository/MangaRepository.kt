package io.tl.mynhentai.data.repository

import io.tl.mynhentai.data.api.CdnRepository
import io.tl.mynhentai.data.api.MangaService
import io.tl.mynhentai.data.local.BlacklistedTagEntity
import io.tl.mynhentai.data.local.FavoriteEntity
import io.tl.mynhentai.data.local.MangaDao
import io.tl.mynhentai.data.model.MangaDetail
import io.tl.mynhentai.data.model.MangaSummary
import kotlinx.coroutines.flow.Flow

class MangaRepository(
    private val api: MangaService,
    private val cdnRepository: CdnRepository,
    private val dao: MangaDao
) {
    suspend fun getPopular(page: Int = 1): List<MangaSummary> = api.getPopular(page)

    suspend fun getGalleries(page: Int = 1, sort: String = "date") =
        api.getGalleries(page, sort)

    suspend fun getDetail(id: Long): MangaDetail = api.getGalleryDetail(id)

    suspend fun search(query: String, page: Int = 1) = api.search(query, page)

    suspend fun getRandom(): MangaSummary = api.getRandom()

    suspend fun refreshCdn() = cdnRepository.refresh()

    fun resolveThumbnailUrl(path: String): String = cdnRepository.resolveThumbnailUrl(path)

    fun resolveImageUrl(path: String): String = cdnRepository.resolveImageUrl(path)

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = dao.getAllFavorites()

    fun isFavorite(id: Long): Flow<Boolean> = dao.isFavorite(id)

    suspend fun getFavorite(id: Long): FavoriteEntity? = dao.getFavorite(id)

    suspend fun addFavorite(favorite: FavoriteEntity) = dao.insert(favorite)

    suspend fun removeFavorite(id: Long) = dao.deleteById(id)

    fun getAllBlacklistedTags(): Flow<List<BlacklistedTagEntity>> = dao.getAllBlacklistedTags()

    suspend fun isTagBlacklisted(tagId: Long): Boolean = dao.isTagBlacklisted(tagId)

    suspend fun addBlacklistedTag(tag: BlacklistedTagEntity) = dao.insertBlacklistedTag(tag)

    suspend fun removeBlacklistedTag(tagId: Long) = dao.removeBlacklistedTag(tagId)

    suspend fun getAllBlacklistedTagIds(): List<Long> = dao.getAllBlacklistedTagIds()
}
