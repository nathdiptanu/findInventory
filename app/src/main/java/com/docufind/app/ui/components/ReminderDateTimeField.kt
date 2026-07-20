package com.docufind.app.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.reminder.ReminderDateTimeFormatter
import com.docufind.app.reminder.ReminderTriggerCalculator

/**
 * Combined date + time picker for reminder scheduling.
 * Uses [DocuFindDateField] for dates and Material 3 [TimePicker] for time.
 */
@Composable
fun ReminderDateTimeField(
    dateMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    timeMinutes: Int,
    onTimeMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String = stringResource(R.string.field_date),
    allowClearDate: Boolean = true,
    showPastWarning: Boolean = true,
    nowMillis: Long = System.currentTimeMillis()
) {
    val context = LocalContext.current
    val use24Hour = DateFormat.is24HourFormat(context)
    val triggerAt = dateMillis?.let { date ->
        val dateMidnight = ReminderTriggerCalculator.dateAtMidnight(date)
        ReminderTriggerCalculator.combineDateAndTime(dateMidnight, timeMinutes)
    }
    val isFutureReminder = triggerAt?.let { ReminderTriggerCalculator.isFutureTrigger(it, nowMillis) } == true

    Column(modifier = modifier.fillMaxWidth()) {
        dateMillis?.let { date ->
            DocuFindDateField(
                label = dateLabel,
                epochMillis = date,
                onDateSelected = { selected -> onDateSelected(selected ?: date) },
                allowClear = allowClearDate,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        ReminderTimePickerButton(
            timeMinutes = timeMinutes,
            onTimeMinutesChange = onTimeMinutesChange,
            use24Hour = use24Hour,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        triggerAt?.let { trigger ->
            Text(
                text = ReminderDateTimeFormatter.formatDateTime(trigger, use24Hour),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (showPastWarning && dateMillis != null && !isFutureReminder) {
            Text(
                text = stringResource(R.string.reminder_future_time_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun ReminderTimePickerButton(
    timeMinutes: Int,
    onTimeMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_time),
    use24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current)
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeLabel = remember(timeMinutes, use24Hour) {
        ReminderDateTimeFormatter.formatTime(timeMinutes, use24Hour)
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialMinutes = timeMinutes,
            use24Hour = use24Hour,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                onTimeMinutesChange(minutes)
                showTimePicker = false
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Text(
                text = timeLabel,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialMinutes: Int,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = use24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
