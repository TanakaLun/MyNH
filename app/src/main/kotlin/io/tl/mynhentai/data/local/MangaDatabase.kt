package io.tl.mynhentai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        BlacklistedTagEntity::class,
        ReadProgressEntity::class,
        HistoryEntity::class,
        UsageEventEntity::class,
        ReadingRecordEntity::class,
        PendingSyncEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class MangaDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun usageDao(): UsageDao
    abstract fun syncDao(): SyncDao
}
