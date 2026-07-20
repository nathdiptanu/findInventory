package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_events",
    indices = [
        Index(value = ["type"]),
        Index(value = ["timestamp"]),
        Index(value = ["screen"]),
        Index(value = ["category"])
    ]
)
data class ActivityEvent(
    @PrimaryKey val id: String,
    val type: String,
    val timestamp: Long,
    val durationMs: Long? = null,
    val screen: String? = null,
    val category: String? = null,
    val metadata: String? = null
)
