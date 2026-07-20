package com.docufind.app.reminder

import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.data.local.db.entity.PetRecord
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.domain.model.pets.PetRecordType
import com.docufind.app.domain.model.reminder.MedicineTimeSlot
import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.insights.ActivityInsightsTracker
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class ReminderUpsertRequest(
    val sourceKey: String,
    val title: String,
    val category: ReminderCategory,
    val reminderDate: Long,
    val reminderTimeMinutes: Int = ReminderScheduleDefaults.DEFAULT_TIME_MINUTES,
    val repeatType: ReminderRepeatType = ReminderRepeatType.NO_REPEAT,
    val importance: ReminderImportance = ReminderImportance.NORMAL,
    val linkedRecordId: String? = null,
    val linkedPetId: String? = null,
    val linkedFamilyMemberId: String? = null,
    val linkedPetRecordId: String? = null,
    val linkedMedicineId: String? = null,
    val notes: String? = null
)

@Singleton
class ReminderEngine @Inject constructor(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val preferencesDataStore: PreferencesDataStore,
    private val activityInsightsTracker: ActivityInsightsTracker
) {
    suspend fun upsert(request: ReminderUpsertRequest): Reminder {
        val existing = reminderDao.getBySourceKey(request.sourceKey)
        if (
            existing?.actionedAt != null &&
            existing.status == ReminderStatus.COMPLETED.name &&
            isLinkedAutoSourceKey(request.sourceKey)
        ) {
            return existing
        }

        val now = System.currentTimeMillis()
        val dateMidnight = ReminderTriggerCalculator.dateAtMidnight(request.reminderDate)
        val triggerAt = ReminderTriggerCalculator.combineDateAndTime(dateMidnight, request.reminderTimeMinutes)
        val reminder = Reminder(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = request.title,
            linkedRecordId = request.linkedRecordId,
            linkedPetId = request.linkedPetId,
            linkedFamilyMemberId = request.linkedFamilyMemberId,
            linkedPetRecordId = request.linkedPetRecordId,
            linkedMedicineId = request.linkedMedicineId,
            category = request.category.name,
            reminderDate = dateMidnight,
            reminderTimeMinutes = request.reminderTimeMinutes,
            triggerAt = triggerAt,
            repeatType = request.repeatType.name,
            importance = request.importance.name,
            status = ReminderStatus.ACTIVE.name,
            sourceKey = request.sourceKey,
            notes = request.notes,
            createdAt = existing?.createdAt ?: now,
            actionedAt = null
        )

        existing?.let { alarmScheduler.cancel(it.id) }
        reminderDao.upsert(reminder)
        if (existing == null) {
            activityInsightsTracker.trackReminderCreated(reminder.category)
        }
        if (triggerAt > now) alarmScheduler.schedule(reminder)
        return reminder
    }

    suspend fun disableByLinkedRecordId(recordId: String) {
        val toCancel = reminderDao.getAllActive().filter { it.linkedRecordId == recordId }
        reminderDao.disableByLinkedRecordId(recordId)
        toCancel.forEach { alarmScheduler.cancel(it.id) }
    }

    suspend fun disableByLinkedPetRecordId(petRecordId: String) {
        val toCancel = reminderDao.getAllActive().filter { it.linkedPetRecordId == petRecordId }
        reminderDao.disableByLinkedPetRecordId(petRecordId)
        toCancel.forEach { alarmScheduler.cancel(it.id) }
    }

    suspend fun syncVaultRecordExpiry(record: VaultRecord, reminderEnabled: Boolean) {
        syncVaultRecordReminders(record, reminderEnabled)
    }

    suspend fun syncVaultRecordReminders(record: VaultRecord, reminderEnabled: Boolean) {
        val recordPrefix = "record:${record.id}"
        if (!reminderEnabled) {
            disableBySourceKeyPrefix(recordPrefix)
            return
        }

        val scheduledPrefixes = mutableSetOf<String>()
        record.expiryDate?.let { dueDate ->
            val prefix = "$recordPrefix:expiry"
            scheduledPrefixes += prefix
            val category = categoryForRecord(record)
            syncOffsetSchedule(
                sourceKeyPrefix = prefix,
                title = "${record.title} - ${category.displayName}",
                category = category,
                dueDateMillis = dueDate,
                importance = importanceForCategory(category),
                linkedRecordId = record.id,
                linkedPetId = record.petId,
                linkedFamilyMemberId = record.familyMemberId,
                notes = record.notes
            )
        }

        metadataDateSchedules(record).forEach { schedule ->
            val prefix = "$recordPrefix:${schedule.key}"
            scheduledPrefixes += prefix
            syncOffsetSchedule(
                sourceKeyPrefix = prefix,
                title = "${record.title} - ${schedule.category.displayName}",
                category = schedule.category,
                dueDateMillis = schedule.dueDateMillis,
                importance = importanceForCategory(schedule.category),
                linkedRecordId = record.id,
                linkedPetId = record.petId,
                linkedFamilyMemberId = record.familyMemberId,
                notes = record.notes
            )
        }

        reminderDao.getAllActive()
            .filter { it.sourceKey.startsWith("$recordPrefix:") }
            .forEach { reminder ->
                val eventPrefix = sourceKeyEventPrefix(reminder.sourceKey)
                if (eventPrefix != null && eventPrefix !in scheduledPrefixes) {
                    reminderDao.upsert(reminder.copy(status = ReminderStatus.DISABLED.name))
                    alarmScheduler.cancel(reminder.id)
                }
            }
    }

    suspend fun syncPetVaccination(record: PetRecord, pet: Pet?) {
        val prefix = "petrec:${record.id}"
        if (record.recordType != PetRecordType.VACCINATION.name || !record.reminderEnabled) {
            disableBySourceKeyPrefix(prefix)
            return
        }
        val dueDate = record.nextDueDate ?: run {
            disableBySourceKeyPrefix(prefix)
            return
        }
        val title = buildString {
            append("Pet Vaccination")
            pet?.name?.let { append(" - $it") }
            record.vaccineName?.let { append(": $it") }
        }
        syncOffsetSchedule(
            sourceKeyPrefix = prefix,
            title = title,
            category = ReminderCategory.PET_VACCINATION,
            dueDateMillis = dueDate,
            importance = ReminderImportance.IMPORTANT,
            linkedPetId = record.petId,
            linkedPetRecordId = record.id,
            notes = record.vetClinic
        )
    }

    suspend fun syncMedicineSchedule(
        medicineId: String,
        medicineName: String,
        familyMemberId: String?,
        petId: String?,
        startDate: Long
    ) {
        MedicineTimeSlot.entries.forEach { slot ->
            upsert(
                ReminderUpsertRequest(
                    sourceKey = "medicine:$medicineId:${slot.name}",
                    title = "$medicineName - ${slot.displayName}",
                    category = ReminderCategory.MEDICINE,
                    reminderDate = startDate,
                    reminderTimeMinutes = slot.minutesFromMidnight,
                    repeatType = ReminderRepeatType.DAILY,
                    importance = ReminderImportance.IMPORTANT,
                    linkedMedicineId = medicineId,
                    linkedFamilyMemberId = familyMemberId,
                    linkedPetId = petId
                )
            )
        }
    }

    suspend fun disableMedicineReminders(medicineId: String) {
        val toCancel = reminderDao.getAllActive().filter { it.linkedMedicineId == medicineId }
        reminderDao.disableByLinkedMedicineId(medicineId)
        toCancel.forEach { alarmScheduler.cancel(it.id) }
    }

    suspend fun rescheduleAllActive() {
        val now = System.currentTimeMillis()
        reminderDao.getAllActive()
            .filter { it.triggerAt > now }
            .forEach { alarmScheduler.schedule(it) }
    }

    suspend fun updateDefaultTimeForActiveOffsetReminders(newTimeMinutes: Int) {
        val now = System.currentTimeMillis()
        reminderDao.getAllActive()
            .filter { sourceKeyEventPrefix(it.sourceKey) != null }
            .forEach { reminder ->
                val updatedTrigger = ReminderTriggerCalculator.combineDateAndTime(
                    reminder.reminderDate,
                    newTimeMinutes
                )
                val updated = reminder.copy(
                    reminderTimeMinutes = newTimeMinutes,
                    triggerAt = updatedTrigger
                )
                alarmScheduler.cancel(reminder.id)
                reminderDao.upsert(updated)
                if (updatedTrigger > now) {
                    alarmScheduler.schedule(updated)
                }
            }
    }

    suspend fun completeReminderEvent(reminderId: String) {
        val reminder = reminderDao.getById(reminderId) ?: return
        findEventGroup(reminder).forEach { r ->
            reminderDao.markCompleted(r.id)
            alarmScheduler.cancel(r.id)
        }
        activityInsightsTracker.trackReminderCompleted(reminder.category)
    }

    suspend fun snoozeReminder(reminderId: String, snoozeDays: Int = ReminderScheduleDefaults.SNOOZE_DAYS) {
        val reminder = reminderDao.getById(reminderId) ?: return
        val snoozed = Instant.ofEpochMilli(reminder.triggerAt)
            .atZone(ZoneId.systemDefault())
            .plusDays(snoozeDays.toLong())
        val newTrigger = snoozed.toInstant().toEpochMilli()
        val updated = reminder.copy(
            triggerAt = newTrigger,
            reminderDate = ReminderTriggerCalculator.dateAtMidnight(newTrigger),
            status = ReminderStatus.ACTIVE.name,
            actionedAt = null
        )
        alarmScheduler.cancel(reminderId)
        reminderDao.upsert(updated)
        if (newTrigger > System.currentTimeMillis()) alarmScheduler.schedule(updated)
    }

    suspend fun onReminderFired(reminderId: String) {
        val reminder = reminderDao.getById(reminderId) ?: return
        val repeatType = ReminderRepeatType.fromStored(reminder.repeatType)
        val next = ReminderTriggerCalculator.nextTrigger(reminder.triggerAt, repeatType)
        if (next != null) {
            val updated = reminder.copy(
                triggerAt = next,
                reminderDate = ReminderTriggerCalculator.dateAtMidnight(next),
                status = ReminderStatus.ACTIVE.name
            )
            reminderDao.upsert(updated)
            alarmScheduler.schedule(updated)
        } else {
            alarmScheduler.cancel(reminderId)
        }
    }

    private suspend fun syncOffsetSchedule(
        sourceKeyPrefix: String,
        title: String,
        category: ReminderCategory,
        dueDateMillis: Long,
        importance: ReminderImportance,
        linkedRecordId: String? = null,
        linkedPetId: String? = null,
        linkedFamilyMemberId: String? = null,
        linkedPetRecordId: String? = null,
        notes: String? = null
    ) {
        disableLegacySingleKey(sourceKeyPrefix)
        val reminderTimeMinutes = preferencesDataStore.preferences.first().defaultReminderTimeMinutes
        ReminderScheduleDefaults.OFFSET_DAYS_BEFORE.forEach { offsetDays ->
            val triggerAt = ReminderTriggerCalculator.triggerBeforeDueDate(
                dueDateMillis,
                offsetDays,
                reminderTimeMinutes
            )
            upsert(
                ReminderUpsertRequest(
                    sourceKey = "$sourceKeyPrefix:$offsetDays",
                    title = title,
                    category = category,
                    reminderDate = ReminderTriggerCalculator.dateAtMidnight(triggerAt),
                    reminderTimeMinutes = reminderTimeMinutes,
                    repeatType = ReminderRepeatType.NO_REPEAT,
                    importance = importance,
                    linkedRecordId = linkedRecordId,
                    linkedPetId = linkedPetId,
                    linkedFamilyMemberId = linkedFamilyMemberId,
                    linkedPetRecordId = linkedPetRecordId,
                    notes = notes
                )
            )
        }

        val expectedKeys = ReminderScheduleDefaults.OFFSET_DAYS_BEFORE
            .map { "$sourceKeyPrefix:$it" }
            .toSet()
        reminderDao.getAllActive()
            .filter { it.sourceKey.startsWith("$sourceKeyPrefix:") && it.sourceKey !in expectedKeys }
            .forEach { stale ->
                reminderDao.upsert(stale.copy(status = ReminderStatus.DISABLED.name))
                alarmScheduler.cancel(stale.id)
            }
    }

    private suspend fun disableLegacySingleKey(prefix: String) {
        reminderDao.getBySourceKey(prefix)?.let { legacy ->
            reminderDao.upsert(legacy.copy(status = ReminderStatus.DISABLED.name))
            alarmScheduler.cancel(legacy.id)
        }
    }

    private suspend fun disableBySourceKeyPrefix(prefix: String) {
        reminderDao.getAllActive()
            .filter { it.sourceKey == prefix || it.sourceKey.startsWith("$prefix:") }
            .forEach { r ->
                reminderDao.upsert(r.copy(status = ReminderStatus.DISABLED.name))
                alarmScheduler.cancel(r.id)
            }
        disableLegacySingleKey(prefix)
    }

    private suspend fun findEventGroup(reminder: Reminder): List<Reminder> {
        val prefix = sourceKeyEventPrefix(reminder.sourceKey)
            ?: reminder.linkedPetRecordId?.let { "petrec:$it" }
            ?: reminder.linkedRecordId?.let { "record:$it:expiry" }
            ?: return listOf(reminder)

        return reminderDao.getAllActive()
            .filter { it.sourceKey == prefix || it.sourceKey.startsWith("$prefix:") }
            .ifEmpty { listOf(reminder) }
    }

    private fun sourceKeyEventPrefix(sourceKey: String): String? {
        val parts = sourceKey.split(":")
        if (parts.size < 3) return null
        val last = parts.last()
        if (last.toIntOrNull() == null) return null
        return parts.dropLast(1).joinToString(":")
    }

    private fun isLinkedAutoSourceKey(sourceKey: String): Boolean =
        sourceKey.startsWith("record:") ||
            sourceKey.startsWith("petrec:") ||
            sourceKey.startsWith("medicine:")

    private data class MetadataReminderSchedule(
        val key: String,
        val category: ReminderCategory,
        val dueDateMillis: Long
    )

    private fun metadataDateSchedules(record: VaultRecord): List<MetadataReminderSchedule> {
        val metadata = parseMetadata(record.tags)
        return when (record.category) {
            "vehicle" -> listOfNotNull(
                metadataDate(metadata, "insurance_expiry")?.let {
                    MetadataReminderSchedule("vehicle_insurance", ReminderCategory.VEHICLE_INSURANCE, it)
                },
                metadataDate(metadata, "puc_expiry")?.let {
                    MetadataReminderSchedule("puc", ReminderCategory.PUC_RENEWAL, it)
                }
            )
            "warranty" -> listOfNotNull(
                metadataDate(metadata, "warranty_expiry")?.let {
                    MetadataReminderSchedule("warranty", ReminderCategory.WARRANTY_EXPIRY, it)
                }
            )
            "insurance" -> listOfNotNull(
                metadataDate(metadata, "renewal_date")?.let {
                    MetadataReminderSchedule("insurance", ReminderCategory.INSURANCE_RENEWAL, it)
                }
            )
            "prescriptions" -> listOfNotNull(
                metadataDate(metadata, "refill_date")?.let {
                    MetadataReminderSchedule("refill", ReminderCategory.PRESCRIPTION_REFILL, it)
                },
                metadataDate(metadata, "follow_up_date")?.let {
                    MetadataReminderSchedule("followup", ReminderCategory.PRESCRIPTION_FOLLOWUP, it)
                }
            )
            "vaccination" -> listOfNotNull(
                metadataDate(metadata, "next_due")?.let {
                    MetadataReminderSchedule("vaccination", ReminderCategory.VACCINATION, it)
                }
            )
            "pets" -> listOfNotNull(
                metadataDate(metadata, "next_vaccination")?.let {
                    MetadataReminderSchedule("pet_vaccination", ReminderCategory.PET_VACCINATION, it)
                }
            )
            else -> emptyList()
        }
    }

    private fun parseMetadata(tags: List<String>): Map<String, String> = tags
        .filter { it.startsWith(METADATA_PREFIX) }
        .mapNotNull { tag ->
            val body = tag.removePrefix(METADATA_PREFIX)
            val idx = body.indexOf('=')
            if (idx <= 0) null else body.substring(0, idx) to body.substring(idx + 1)
        }
        .toMap()

    private fun metadataDate(metadata: Map<String, String>, key: String): Long? =
        metadata[key]?.toLongOrNull()

    private fun categoryForRecord(record: VaultRecord): ReminderCategory = when (record.category) {
        "prescriptions" -> if (record.tags.any { it.contains("follow", ignoreCase = true) }) {
            ReminderCategory.PRESCRIPTION_FOLLOWUP
        } else {
            ReminderCategory.PRESCRIPTION_REFILL
        }
        "vaccination" -> ReminderCategory.VACCINATION
        "insurance" -> ReminderCategory.INSURANCE_RENEWAL
        "vehicle" -> if (
            record.subCategory?.contains("PUC", ignoreCase = true) == true ||
            record.tags.any { it.contains("puc", ignoreCase = true) }
        ) {
            ReminderCategory.PUC_RENEWAL
        } else {
            ReminderCategory.VEHICLE_INSURANCE
        }
        "warranty" -> ReminderCategory.WARRANTY_EXPIRY
        "id_cards" -> ReminderCategory.PASSPORT_ID_EXPIRY
        "education" -> ReminderCategory.EDUCATION
        "medical" -> ReminderCategory.MEDICINE
        else -> ReminderCategory.CUSTOM
    }

    private fun importanceForCategory(category: ReminderCategory): ReminderImportance = when (category) {
        ReminderCategory.MEDICINE,
        ReminderCategory.PRESCRIPTION_REFILL,
        ReminderCategory.PET_VACCINATION -> ReminderImportance.IMPORTANT
        ReminderCategory.PRESCRIPTION_FOLLOWUP,
        ReminderCategory.VACCINATION -> ReminderImportance.URGENT
        else -> ReminderImportance.NORMAL
    }

    private companion object {
        const val METADATA_PREFIX = "meta:"
    }
}
