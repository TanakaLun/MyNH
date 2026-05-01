package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklisted_tags")
data class BlacklistedTagEntity(
    @PrimaryKey val tagId: Long,
    val tagName: String,
    val addedAt: Long = System.currentTimeMillis()
)
