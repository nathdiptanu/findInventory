package com.docufind.app.unit.reminders

import com.docufind.app.data.repository.ReminderRepositoryImpl
import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.domain.model.reminder.CustomReminderRequest
import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.reminder.ReminderAlarmScheduler
import com.docufind.app.reminder.ReminderEngine
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReminderRepositoryImplTest {

    private val reminderDao = mockk<ReminderDao>(relaxed = true)
    private val reminderEngine = mockk<ReminderEngine>(relaxed = true)
    private val alarmScheduler = mockk<ReminderAlarmScheduler>(relaxed = true)
    private lateinit var repository: ReminderRepositoryImpl

    @Before
    fun setup() {
        repository = ReminderRepositoryImpl(reminderDao, reminderEngine, alarmScheduler)
    }

    @Test
    fun saveCustomReminder_rejectsPastTrigger() = runTest {
        val zone = ZoneId.systemDefault()
        val yesterday = LocalDate.now(zone).minusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val result = repository.saveCustomReminder(
            CustomReminderRequest(
                title = "Past reminder",
                category = ReminderCategory.CUSTOM,
                reminderDate = yesterday,
                reminderTimeMinutes = 9 * 60,
                repeatType = ReminderRepeatType.NO_REPEAT,
                importance = ReminderImportance.NORMAL,
                notes = null
            )
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("future")
    }

    @Test
    fun saveCustomReminder_acceptsFutureTrigger() = runTest {
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now(zone).plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val result = repository.saveCustomReminder(
            CustomReminderRequest(
                title = "Future reminder",
                category = ReminderCategory.CUSTOM,
                reminderDate = tomorrow,
                reminderTimeMinutes = 9 * 60,
                repeatType = ReminderRepeatType.NO_REPEAT,
                importance = ReminderImportance.NORMAL,
                notes = null
            )
        )

        assertThat(result.isSuccess).isTrue()
    }
}
