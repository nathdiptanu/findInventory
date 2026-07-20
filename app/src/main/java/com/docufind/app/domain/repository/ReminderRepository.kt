package com.docufind.app.domain.repository

import com.docufind.app.domain.model.reminder.CustomReminderRequest
import com.docufind.app.domain.model.reminder.ReminderListItem
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeActiveCount(): Flow<Int>
    fun observeAll(): Flow<List<ReminderListItem>>
    suspend fun saveCustomReminder(request: CustomReminderRequest): Result<Unit>
    suspend fun updateReminder(id: String, request: CustomReminderRequest): Result<Unit>
    suspend fun updateDefaultTimeForLinkedReminders(minutes: Int): Result<Unit>
    suspend fun markCompleted(reminderId: String)
    suspend fun deleteReminder(reminderId: String)
}
