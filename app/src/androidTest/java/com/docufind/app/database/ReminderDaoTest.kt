package com.docufind.app.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var db: com.docufind.app.data.local.db.DocuFindDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DocuFindInMemoryDatabase.create(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertReminder_andFetchById() = runBlocking {
        val reminder = TestFixtures.reminder(id = "dao-rem-1")
        db.reminderDao().upsert(reminder)
        assertThat(db.reminderDao().getById("dao-rem-1")?.title).isEqualTo(reminder.title)
    }

    @Test
    fun markCompleted_setsStatusAndActionedAt() = runBlocking {
        val reminder = TestFixtures.reminder(id = "dao-rem-done")
        db.reminderDao().upsert(reminder)
        db.reminderDao().markCompleted("dao-rem-done", actionedAt = 42_000L)
        val updated = db.reminderDao().getById("dao-rem-done")
        assertThat(updated?.status).isEqualTo(ReminderStatus.COMPLETED.name)
        assertThat(updated?.actionedAt).isEqualTo(42_000L)
    }

    @Test
    fun deleteReminder_removesRow() = runBlocking {
        val reminder = TestFixtures.reminder(id = "dao-rem-del")
        db.reminderDao().upsert(reminder)
        db.reminderDao().deleteById("dao-rem-del")
        assertThat(db.reminderDao().getById("dao-rem-del")).isNull()
    }
}
