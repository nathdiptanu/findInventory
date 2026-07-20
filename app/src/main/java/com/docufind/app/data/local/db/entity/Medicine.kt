package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicines",
    indices = [
        Index(value = ["name"]),
        Index(value = ["familyMemberId"]),
        Index(value = ["petId"])
    ]
)
data class Medicine(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String? = null,
    val familyMemberId: String? = null,
    val petId: String? = null,
    val scheduleNotes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
