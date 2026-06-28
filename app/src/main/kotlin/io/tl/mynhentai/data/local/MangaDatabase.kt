package io.tl.mynhentai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class, BlacklistedTagEntity::class, ReadProgressEntity::class, HistoryEntity::class], version = 4, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
}
