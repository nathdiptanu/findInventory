package com.docufind.app.reminder

/**
 * Default advance reminder schedule for expiry/due-date events.
 * User-configurable offsets may be added later; these are the product defaults.
 */
object ReminderScheduleDefaults {
    /** Days before due date, plus 0 = due date morning. */
    val OFFSET_DAYS_BEFORE: List<Int> = listOf(15, 7, 3, 2, 1, 0)

    const val DEFAULT_TIME_MINUTES: Int = 9 * 60 // 9:00 AM local

    const val SNOOZE_DAYS: Int = 1
}
