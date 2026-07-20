package com.docufind.app.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderDateTimeFormatter {
    fun formatTime(minutes: Int, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return LocalTime.of(minutes / 60, minutes % 60).format(formatter)
    }

    fun formatDateTime(epochMillis: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "d MMM yyyy · HH:mm" else "d MMM yyyy · h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
