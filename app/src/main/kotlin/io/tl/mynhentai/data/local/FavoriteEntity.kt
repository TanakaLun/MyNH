package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val thumbnail: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val numPages: Int,
    val addedAt: Long = System.currentTimeMillis()
)
