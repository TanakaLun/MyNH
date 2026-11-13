package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One reading session. Tags are stored as a compact JSON snapshot so that the
 * reading-taste profile can be rebuilt without extra joins.
 */
@Serializable
@Entity(
    tableName = "reading_record",
    indices = [Index("startedAt"), Index("galleryId")]
)
data class ReadingRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val galleryId: Long,
    val title: String,
    val coverPath: String? = null,
    val startedAt: Long,
    val durationMillis: Long = 0,
    val pageCount: Int = 0,
    val lastPage: Int = 1,
    val tagsJson: String = "[]",
    val language: String? = null,
)
