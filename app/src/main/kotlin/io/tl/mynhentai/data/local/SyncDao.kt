package io.tl.mynhentai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync WHERE kind = :kind ORDER BY id ASC")
    suspend fun getByKind(kind: String): List<PendingSyncEntity>

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_sync WHERE kind = :kind AND targetId = :targetId")
    suspend fun deleteByTarget(kind: String, targetId: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observeCount(): Flow<Int>
}
