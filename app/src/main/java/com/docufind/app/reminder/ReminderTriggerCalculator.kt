package com.docufind.app.reminder

import com.docufind.app.domain.model.reminder.ReminderRepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReminderTriggerCalculator {
    private const val ALARM_REQUEST_CODE_PREFIX = "docufind_reminder_alarm:"

    fun combineDateAndTime(dateMillis: Long, timeMinutes: Int): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        val hour = timeMinutes / 60
        val minute = timeMinutes % 60
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    fun dateAtMidnight(epochMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun nextTrigger(currentTriggerAt: Long, repeatType: ReminderRepeatType): Long? {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(currentTriggerAt).atZone(zone)
        val next = when (repeatType) {
            ReminderRepeatType.DAILY -> current.plusDays(1)
            ReminderRepeatType.WEEKLY -> current.plusWeeks(1)
            ReminderRepeatType.MONTHLY -> current.plusMonths(1)
            ReminderRepeatType.YEARLY -> current.plusYears(1)
            ReminderRepeatType.CUSTOM -> current.plusMonths(1)
            ReminderRepeatType.NO_REPEAT -> return null
        }
        return next.toInstant().toEpochMilli()
    }

    fun todayMidnight(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun isFutureTrigger(triggerAt: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        triggerAt > nowMillis

    fun isFutureDateTime(dateMillis: Long, timeMinutes: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val dateMidnight = dateAtMidnight(dateMillis)
        val triggerAt = combineDateAndTime(dateMidnight, timeMinutes)
        return isFutureTrigger(triggerAt, nowMillis)
    }

    /** Stable PendingIntent request code for a reminder alarm. */
    fun stableAlarmRequestCode(reminderId: String): Int =
        (ALARM_REQUEST_CODE_PREFIX + reminderId).hashCode() and 0x7FFFFFFF

    /** Returns trigger millis for [daysBefore] days prior to the due date at [timeMinutes]. */
    fun triggerBeforeDueDate(dueDateMillis: Long, daysBefore: Int, timeMinutes: Int = ReminderScheduleDefaults.DEFAULT_TIME_MINUTES): Long {
        val zone = ZoneId.systemDefault()
        val dueDate = Instant.ofEpochMilli(dueDateMillis).atZone(zone).toLocalDate()
        val reminderDate = dueDate.minusDays(daysBefore.toLong())
        val midnight = reminderDate.atStartOfDay(zone).toInstant().toEpochMilli()
        return combineDateAndTime(midnight, timeMinutes)
    }
}
