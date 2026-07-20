package com.docufind.app.unit.reminders

import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.domain.model.AppPreferences
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.reminder.ReminderAlarmScheduler
import com.docufind.app.reminder.ReminderEngine
import com.docufind.app.reminder.ReminderScheduleDefaults
import com.docufind.app.insights.ActivityInsightsTracker
import com.docufind.app.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ReminderEngineTest {

    private val reminderDao = mockk<ReminderDao>(relaxed = true)
    private val alarmScheduler = mockk<ReminderAlarmScheduler>(relaxed = true)
    private val preferencesDataStore = mockk<PreferencesDataStore>()
    private val activityInsightsTracker = mockk<ActivityInsightsTracker>(relaxed = true)
    private lateinit var engine: ReminderEngine

    @Before
    fun setup() {
        every { preferencesDataStore.preferences } returns flowOf(AppPreferences())
        engine = ReminderEngine(reminderDao, alarmScheduler, preferencesDataStore, activityInsightsTracker)
    }

    @Test
    fun syncVaultRecordExpiry_createsSixOffsetReminders() = runTest {
        val record = TestFixtures.vaultRecord(
            expiryDate = TestFixtures.epoch(LocalDate.of(2026, 8, 1))
        )
        coEvery { reminderDao.getBySourceKey(any()) } returns null
        coEvery { reminderDao.getAllActive() } returns emptyList()

        engine.syncVaultRecordExpiry(record, reminderEnabled = true)

        coVerify(exactly = 6) { reminderDao.upsert(any()) }
        ReminderScheduleDefaults.OFFSET_DAYS_BEFORE.forEach { offset ->
            coVerify { reminderDao.getBySourceKey("record:${record.id}:expiry:$offset") }
        }
    }

    @Test
    fun syncVaultRecordExpiry_disabled_cancelsExistingReminders() = runTest {
        val record = TestFixtures.vaultRecord()
        val active = listOf(
            TestFixtures.reminder(id = "r1", sourceKey = "record:rec-1:expiry:15"),
            TestFixtures.reminder(id = "r2", sourceKey = "record:rec-1:expiry:7")
        )
        val disabledUpserts = mutableListOf<com.docufind.app.data.local.db.entity.Reminder>()
        coEvery { reminderDao.getAllActive() } returns active
        coEvery { reminderDao.getBySourceKey(any()) } returns null
        coEvery { reminderDao.upsert(any()) } answers {
            disabledUpserts.add(firstArg())
        }

        engine.syncVaultRecordExpiry(record, reminderEnabled = false)

        assertThat(disabledUpserts).hasSize(2)
        assertThat(disabledUpserts.map { it.id }).containsExactly("r1", "r2")
        assertThat(disabledUpserts.all { it.status == ReminderStatus.DISABLED.name }).isTrue()
        coVerify { alarmScheduler.cancel("r1") }
        coVerify { alarmScheduler.cancel("r2") }
    }

    @Test
    fun completeReminderEvent_marksEntireLinkedGroupActioned() = runTest {
        val group = listOf(
            TestFixtures.reminder(id = "r15", sourceKey = "record:rec-1:expiry:15"),
            TestFixtures.reminder(id = "r7", sourceKey = "record:rec-1:expiry:7"),
            TestFixtures.reminder(id = "r0", sourceKey = "record:rec-1:expiry:0")
        )
        coEvery { reminderDao.getById("r15") } returns group.first()
        coEvery { reminderDao.getAllActive() } returns group

        engine.completeReminderEvent("r15")

        coVerify { reminderDao.markCompleted("r15", any()) }
        coVerify { reminderDao.markCompleted("r7", any()) }
        coVerify { reminderDao.markCompleted("r0", any()) }
        coVerify { alarmScheduler.cancel("r15") }
        coVerify { alarmScheduler.cancel("r7") }
        coVerify { alarmScheduler.cancel("r0") }
    }

    @Test
    fun upsert_schedulesAlarmWhenTriggerInFuture() = runTest {
        val futureDate = System.currentTimeMillis() + 86_400_000L * 30
        val upsertSlot = slot<com.docufind.app.data.local.db.entity.Reminder>()
        coEvery { reminderDao.getBySourceKey(any()) } returns null
        coEvery { reminderDao.upsert(capture(upsertSlot)) } answers { }

        engine.upsert(
            com.docufind.app.reminder.ReminderUpsertRequest(
                sourceKey = "custom:test",
                title = "Test",
                category = com.docufind.app.domain.model.reminder.ReminderCategory.CUSTOM,
                reminderDate = futureDate
            )
        )

        assertThat(upsertSlot.captured.sourceKey).isEqualTo("custom:test")
        coVerify { alarmScheduler.schedule(any()) }
    }
}
