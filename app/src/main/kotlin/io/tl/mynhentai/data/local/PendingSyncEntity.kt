package io.tl.mynhentai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object PendingKind {
    const val FAVORITE_ADD = "favorite_add"
    const val FAVORITE_REMOVE = "favorite_remove"
    const val BLACKLIST_ADD = "blacklist_add"
    const val BLACKLIST_REMOVE = "blacklist_remove"
}

/**
 * Local change that could not be pushed to the server yet. At most one row exists per
 * (kind, targetId) pair; enqueueing the opposite kind first removes the stale one.
 */
@Entity(
    tableName = "pending_sync",
    indices = [Index(value = ["kind", "targetId"], unique = true)]
)
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val targetId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
