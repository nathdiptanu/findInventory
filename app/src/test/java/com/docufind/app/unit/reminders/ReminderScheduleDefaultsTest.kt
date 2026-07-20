package com.docufind.app.unit.reminders

import com.docufind.app.reminder.ReminderScheduleDefaults
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReminderScheduleDefaultsTest {

    @Test
    fun offsetDays_includesFifteenSevenThreeTwoOneAndDueDay() {
        assertThat(ReminderScheduleDefaults.OFFSET_DAYS_BEFORE)
            .containsExactly(15, 7, 3, 2, 1, 0)
            .inOrder()
    }

    @Test
    fun defaultTime_isNineAm() {
        assertThat(ReminderScheduleDefaults.DEFAULT_TIME_MINUTES).isEqualTo(9 * 60)
    }

    @Test
    fun snoozeDefault_isOneDay() {
        assertThat(ReminderScheduleDefaults.SNOOZE_DAYS).isEqualTo(1)
    }
}
