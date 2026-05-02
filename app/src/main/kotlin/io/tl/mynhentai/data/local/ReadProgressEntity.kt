package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_progress")
data class ReadProgressEntity(
    @PrimaryKey val galleryId: Long,
    val pageNumber: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
