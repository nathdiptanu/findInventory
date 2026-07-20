package com.docufind.app.reminder

import com.docufind.app.domain.model.reminder.ReminderRepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderTriggerCalculatorTest {

    @Test
    fun combineDateAndTime_mergesCorrectly() {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.of(2026, 6, 28).atStartOfDay(zone).toInstant().toEpochMilli()
        val trigger = ReminderTriggerCalculator.combineDateAndTime(date, 8 * 60)
        val local = java.time.Instant.ofEpochMilli(trigger).atZone(zone)
        assertEquals(8, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun nextTrigger_dailyAdvancesOneDay() {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.of(2026, 6, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val next = ReminderTriggerCalculator.nextTrigger(start, ReminderRepeatType.DAILY)
        requireNotNull(next)
        val local = java.time.Instant.ofEpochMilli(next).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2026, 6, 29), local)
    }

    @Test
    fun nextTrigger_noRepeatReturnsNull() {
        val now = System.currentTimeMillis()
        assertNull(ReminderTriggerCalculator.nextTrigger(now, ReminderRepeatType.NO_REPEAT))
    }

    @Test
    fun triggerBeforeDueDate_offsetsFromDueDate() {
        val zone = ZoneId.systemDefault()
        val dueDate = LocalDate.of(2026, 7, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        val trigger15 = ReminderTriggerCalculator.triggerBeforeDueDate(dueDate, 15)
        val local15 = java.time.Instant.ofEpochMilli(trigger15).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2026, 6, 30), local15)
        val trigger0 = ReminderTriggerCalculator.triggerBeforeDueDate(dueDate, 0)
        val local0 = java.time.Instant.ofEpochMilli(trigger0).atZone(zone)
        assertEquals(9, local0.hour)
        assertEquals(LocalDate.of(2026, 7, 15), local0.toLocalDate())
    }

    @Test
    fun isFutureTrigger_rejectsPastAndAcceptsFuture() {
        val now = 1_700_000_000_000L
        assertEquals(false, ReminderTriggerCalculator.isFutureTrigger(now - 1, now))
        assertEquals(true, ReminderTriggerCalculator.isFutureTrigger(now + 1, now))
    }

    @Test
    fun isFutureDateTime_validatesCombinedDateAndTime() {
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now(zone).plusDays(1)
        val dateMillis = tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()
        assertTrue(ReminderTriggerCalculator.isFutureDateTime(dateMillis, 9 * 60))
    }

    @Test
    fun stableAlarmRequestCode_isStableAndDistinct() {
        val idA = "550e8400-e29b-41d4-a716-446655440000"
        val idB = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
        val codeA1 = ReminderTriggerCalculator.stableAlarmRequestCode(idA)
        val codeA2 = ReminderTriggerCalculator.stableAlarmRequestCode(idA)
        assertEquals(codeA1, codeA2)
        assertTrue(codeA1 >= 0)
        assertNotEquals(
            ReminderTriggerCalculator.stableAlarmRequestCode(idA),
            ReminderTriggerCalculator.stableAlarmRequestCode(idB)
        )
    }
}
