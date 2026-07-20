package com.docufind.app.testutil

import com.docufind.app.data.local.db.entity.FamilyMember
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.data.local.db.entity.VaultFile
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.domain.model.reminder.ReminderStatus
import java.time.LocalDate
import java.time.ZoneId

object TestFixtures {
    private val zone = ZoneId.systemDefault()

    fun epoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun vaultRecord(
        id: String = "rec-1",
        title: String = "Policy",
        category: String = "insurance",
        expiryDate: Long? = epoch(LocalDate.of(2026, 12, 31)),
        tags: List<String> = emptyList()
    ) = VaultRecord(
        id = id,
        title = title,
        category = category,
        expiryDate = expiryDate,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        tags = tags
    )

    fun vaultFile(
        id: String = "file-1",
        recordId: String = "rec-1",
        fileName: String = "scan.pdf"
    ) = VaultFile(
        id = id,
        recordId = recordId,
        fileName = fileName,
        mimeType = "application/pdf",
        fileSize = 1024,
        localPath = "vault/$id.enc",
        createdAt = 1_000L
    )

    fun reminder(
        id: String = "rem-1",
        sourceKey: String = "record:rec-1:expiry:15",
        linkedRecordId: String? = "rec-1",
        status: String = ReminderStatus.ACTIVE.name
    ) = Reminder(
        id = id,
        title = "Policy — Insurance",
        linkedRecordId = linkedRecordId,
        category = ReminderCategory.INSURANCE_RENEWAL.name,
        reminderDate = epoch(LocalDate.of(2026, 12, 16)),
        reminderTimeMinutes = 540,
        triggerAt = epoch(LocalDate.of(2026, 12, 16)) + 540 * 60_000L,
        repeatType = ReminderRepeatType.NO_REPEAT.name,
        importance = ReminderImportance.NORMAL.name,
        status = status,
        sourceKey = sourceKey,
        createdAt = 1_000L
    )

    fun familyMember(id: String = "fam-1", name: String = "Jane") = FamilyMember(
        id = id,
        name = name,
        relationship = "Spouse",
        createdAt = 1_000L,
        updatedAt = 1_000L
    )
}
