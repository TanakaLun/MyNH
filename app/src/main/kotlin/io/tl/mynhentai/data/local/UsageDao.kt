package io.tl.mynhentai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Insert
    suspend fun insertEvent(event: UsageEventEntity): Long

    @Query("UPDATE usage_event SET durationMillis = :duration, startAt = :startAt WHERE id = :id")
    suspend fun updateEventDuration(id: Long, duration: Long, startAt: Long)

    @Insert
    suspend fun insertReading(record: ReadingRecordEntity): Long

    @Query("UPDATE reading_record SET durationMillis = :duration, lastPage = :lastPage WHERE id = :id")
    suspend fun updateReading(id: Long, duration: Long, lastPage: Int)

    @Query("SELECT * FROM usage_event WHERE startAt >= :since ORDER BY startAt DESC")
    fun observeEventsSince(since: Long): Flow<List<UsageEventEntity>>

    @Query("SELECT * FROM reading_record WHERE startedAt >= :since ORDER BY startedAt DESC")
    fun observeReadingsSince(since: Long): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM usage_event ORDER BY startAt ASC")
    suspend fun getAllEventsOnce(): List<UsageEventEntity>

    @Query("SELECT * FROM reading_record ORDER BY startedAt ASC")
    suspend fun getAllReadingsOnce(): List<ReadingRecordEntity>

    @Insert
    suspend fun insertEvents(events: List<UsageEventEntity>)

    @Insert
    suspend fun insertReadings(records: List<ReadingRecordEntity>)

    @Query("DELETE FROM usage_event")
    suspend fun clearEvents()

    @Query("DELETE FROM reading_record")
    suspend fun clearReadings()

    @Query("DELETE FROM usage_event WHERE startAt < :cutoff")
    suspend fun deleteEventsBefore(cutoff: Long)

    @Query("DELETE FROM reading_record WHERE startedAt < :cutoff")
    suspend fun deleteReadingsBefore(cutoff: Long)
}
