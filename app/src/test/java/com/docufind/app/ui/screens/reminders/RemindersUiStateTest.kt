package com.docufind.app.ui.screens.reminders

import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderListItem
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.domain.model.reminder.ReminderTab
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemindersUiStateTest {

    @Test
    fun upcomingTab_sortsChronologicallyAndExcludesNonActiveOrPast() {
        val now = System.currentTimeMillis()
        val reminders = listOf(
            item("future-late", now + 20_000, ReminderStatus.ACTIVE),
            item("future-early", now + 10_000, ReminderStatus.ACTIVE),
            item("past", now - 1, ReminderStatus.ACTIVE),
            item("completed", now + 5_000, ReminderStatus.COMPLETED),
            item("disabled", now + 5_000, ReminderStatus.DISABLED)
        )
        val state = RemindersUiState(
            reminders = reminders,
            selectedTab = ReminderTab.UPCOMING
        )

        assertThat(state.filteredReminders.map { it.id }).containsExactly("future-early", "future-late").inOrder()
    }

    @Test
    fun overdueTab_includesOnlyActivePastReminders() {
        val now = System.currentTimeMillis()
        val reminders = listOf(
            item("overdue", now - 60_000, ReminderStatus.ACTIVE),
            item("future", now + 60_000, ReminderStatus.ACTIVE),
            item("completed-past", now - 60_000, ReminderStatus.COMPLETED)
        )
        val state = RemindersUiState(
            reminders = reminders,
            selectedTab = ReminderTab.OVERDUE
        )

        assertThat(state.filteredReminders.map { it.id }).containsExactly("overdue")
    }

    private fun item(id: String, triggerAt: Long, status: ReminderStatus) = ReminderListItem(
        id = id,
        title = id,
        category = ReminderCategory.CUSTOM,
        triggerAt = triggerAt,
        repeatType = ReminderRepeatType.NO_REPEAT,
        importance = ReminderImportance.NORMAL,
        status = status,
        linkedRecordId = null,
        linkedPetId = null,
        linkedPetRecordId = null,
        notes = null
    )
}
