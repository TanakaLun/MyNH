package io.tl.mynhentai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getFavorite(id: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM blacklisted_tags ORDER BY addedAt DESC")
    fun getAllBlacklistedTags(): Flow<List<BlacklistedTagEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklisted_tags WHERE tagId = :tagId)")
    suspend fun isTagBlacklisted(tagId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistedTag(tag: BlacklistedTagEntity)

    @Query("DELETE FROM blacklisted_tags WHERE tagId = :tagId")
    suspend fun removeBlacklistedTag(tagId: Long)

    @Query("SELECT tagId FROM blacklisted_tags")
    suspend fun getAllBlacklistedTagIds(): List<Long>
}
