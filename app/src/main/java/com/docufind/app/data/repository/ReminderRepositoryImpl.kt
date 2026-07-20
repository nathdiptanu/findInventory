package com.docufind.app.data.repository

import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.domain.model.reminder.CustomReminderRequest
import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderListItem
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.domain.repository.ReminderRepository
import com.docufind.app.reminder.ReminderAlarmScheduler
import com.docufind.app.reminder.ReminderEngine
import com.docufind.app.reminder.ReminderTriggerCalculator
import com.docufind.app.reminder.ReminderUpsertRequest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderEngine: ReminderEngine,
    private val alarmScheduler: ReminderAlarmScheduler
) : ReminderRepository {

    override fun observeActiveCount(): Flow<Int> = reminderDao.observeActiveCount()

    override fun observeAll(): Flow<List<ReminderListItem>> =
        reminderDao.observeAll().map { reminders -> reminders.map(::toListItem) }

    override suspend fun saveCustomReminder(request: CustomReminderRequest): Result<Unit> = runCatching {
        validateCustomReminderRequest(request)
        val sourceKey = "custom:${UUID.randomUUID()}"
        reminderEngine.upsert(
            ReminderUpsertRequest(
                sourceKey = sourceKey,
                title = request.title.trim(),
                category = request.category,
                reminderDate = request.reminderDate,
                reminderTimeMinutes = request.reminderTimeMinutes,
                repeatType = request.repeatType,
                importance = request.importance,
                notes = request.notes?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    override suspend fun updateReminder(id: String, request: CustomReminderRequest): Result<Unit> = runCatching {
        validateCustomReminderRequest(request)
        val existing = reminderDao.getById(id) ?: error("Reminder not found")
        val dateMidnight = ReminderTriggerCalculator.dateAtMidnight(request.reminderDate)
        val triggerAt = ReminderTriggerCalculator.combineDateAndTime(dateMidnight, request.reminderTimeMinutes)
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = request.title.trim(),
            category = request.category.name,
            reminderDate = dateMidnight,
            reminderTimeMinutes = request.reminderTimeMinutes,
            triggerAt = triggerAt,
            repeatType = request.repeatType.name,
            importance = request.importance.name,
            status = ReminderStatus.ACTIVE.name,
            notes = request.notes?.trim()?.takeIf { it.isNotEmpty() }
        )
        alarmScheduler.cancel(id)
        reminderDao.upsert(updated)
        if (triggerAt > now) {
            alarmScheduler.schedule(updated)
        }
    }

    override suspend fun updateDefaultTimeForLinkedReminders(minutes: Int): Result<Unit> = runCatching {
        reminderEngine.updateDefaultTimeForActiveOffsetReminders(minutes)
    }

    override suspend fun markCompleted(reminderId: String) {
        reminderEngine.completeReminderEvent(reminderId)
    }

    override suspend fun deleteReminder(reminderId: String) {
        alarmScheduler.cancel(reminderId)
        reminderDao.deleteById(reminderId)
    }

    private fun toListItem(entity: Reminder): ReminderListItem =
        ReminderListItem(
            id = entity.id,
            title = entity.title,
            category = ReminderCategory.fromStored(entity.category),
            triggerAt = entity.triggerAt,
            repeatType = ReminderRepeatType.fromStored(entity.repeatType),
            importance = ReminderImportance.fromStored(entity.importance),
            status = ReminderStatus.fromStored(entity.status),
            linkedRecordId = entity.linkedRecordId,
            linkedPetId = entity.linkedPetId,
            linkedPetRecordId = entity.linkedPetRecordId,
            notes = entity.notes
        )

    private fun validateCustomReminderRequest(request: CustomReminderRequest) {
        require(request.title.isNotBlank()) { "Reminder title is required" }
        require(
            ReminderTriggerCalculator.isFutureDateTime(request.reminderDate, request.reminderTimeMinutes)
        ) { "Choose a future reminder date and time" }
    }
}
