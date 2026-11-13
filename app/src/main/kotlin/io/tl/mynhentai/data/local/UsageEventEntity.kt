package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single record of app usage: foreground time, a gallery view or a search.
 * Reading sessions live in [ReadingRecordEntity] so that tag data is kept per read.
 */
@Serializable
@Entity(
    tableName = "usage_event",
    indices = [Index("startAt"), Index("kind")]
)
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val startAt: Long,
    val durationMillis: Long = 0,
    val galleryId: Long? = null,
    val galleryTitle: String? = null,
    val coverPath: String? = null,
    val query: String? = null,
)
