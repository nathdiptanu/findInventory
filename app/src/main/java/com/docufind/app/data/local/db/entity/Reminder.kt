package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["sourceKey"], unique = true),
        Index(value = ["triggerAt"]),
        Index(value = ["linkedRecordId"]),
        Index(value = ["linkedPetRecordId"]),
        Index(value = ["linkedMedicineId"]),
        Index(value = ["status"])
    ]
)
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val linkedRecordId: String? = null,
    val linkedPetId: String? = null,
    val linkedFamilyMemberId: String? = null,
    val linkedPetRecordId: String? = null,
    val linkedMedicineId: String? = null,
    val category: String,
    val reminderDate: Long,
    val reminderTimeMinutes: Int,
    val triggerAt: Long,
    val repeatType: String,
    val importance: String,
    val status: String,
    val sourceKey: String,
    val notes: String? = null,
    val createdAt: Long,
    /** Epoch millis when user marked this reminder actioned (nullable). */
    val actionedAt: Long? = null
)
