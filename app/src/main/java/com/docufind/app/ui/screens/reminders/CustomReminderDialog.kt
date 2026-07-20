package com.docufind.app.ui.screens.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.domain.model.reminder.CustomReminderRequest
import com.docufind.app.domain.model.reminder.ReminderCategory
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderListItem
import com.docufind.app.domain.model.reminder.ReminderRepeatType
import com.docufind.app.reminder.ReminderTriggerCalculator
import com.docufind.app.ui.components.ReminderDateTimeField
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderDialog(
    onDismiss: () -> Unit,
    onSave: (CustomReminderRequest) -> Unit,
    existing: ReminderListItem? = null
) {
    val initialDateTime = remember(existing?.id) {
        existing?.triggerAt?.let { triggerAt ->
            val zdt = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
            val midnight = zdt.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            midnight to (zdt.hour * 60 + zdt.minute)
        }
    }

    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: ReminderCategory.CUSTOM) }
    var reminderDate by remember(existing?.id) {
        mutableStateOf<Long?>(initialDateTime?.first ?: System.currentTimeMillis())
    }
    var selectedTimeMinutes by remember(existing?.id) { mutableStateOf(initialDateTime?.second ?: 540) }
    var repeatType by remember(existing?.id) {
        mutableStateOf(existing?.repeatType ?: ReminderRepeatType.NO_REPEAT)
    }
    var importance by remember(existing?.id) {
        mutableStateOf(existing?.importance ?: ReminderImportance.NORMAL)
    }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var importanceExpanded by remember { mutableStateOf(false) }

    val dialogTitle = if (existing == null) {
        stringResource(R.string.custom_reminder_title)
    } else {
        stringResource(R.string.edit_reminder_title)
    }
    val triggerAt = reminderDate?.let { date ->
        val dateMidnight = ReminderTriggerCalculator.dateAtMidnight(date)
        ReminderTriggerCalculator.combineDateAndTime(dateMidnight, selectedTimeMinutes)
    }
    val isFutureReminder = triggerAt?.let { ReminderTriggerCalculator.isFutureTrigger(it) } == true
    val canSave = title.trim().isNotEmpty() && isFutureReminder

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                DropdownSelector(
                    label = stringResource(R.string.field_category),
                    value = category.displayName,
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    options = ReminderCategory.entries.map { it.displayName },
                    onSelect = { label ->
                        category = ReminderCategory.entries.first { it.displayName == label }
                        categoryExpanded = false
                    }
                )
                ReminderDateTimeField(
                    dateMillis = reminderDate,
                    onDateSelected = { reminderDate = it },
                    timeMinutes = selectedTimeMinutes,
                    onTimeMinutesChange = { selectedTimeMinutes = it },
                    showPastWarning = title.isNotBlank()
                )
                if (title.isBlank()) {
                    Text(
                        text = stringResource(R.string.reminder_title_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                DropdownSelector(
                    label = stringResource(R.string.field_repeat),
                    value = repeatType.displayName,
                    expanded = repeatExpanded,
                    onExpandedChange = { repeatExpanded = it },
                    options = ReminderRepeatType.selectable.map { it.displayName },
                    onSelect = { label ->
                        repeatType = ReminderRepeatType.selectable.first { it.displayName == label }
                        repeatExpanded = false
                    }
                )
                DropdownSelector(
                    label = stringResource(R.string.field_importance),
                    value = importance.displayName,
                    expanded = importanceExpanded,
                    onExpandedChange = { importanceExpanded = it },
                    options = ReminderImportance.selectable.map { it.displayName },
                    onSelect = { label ->
                        importance = ReminderImportance.selectable.first { it.displayName == label }
                        importanceExpanded = false
                    }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val date = reminderDate ?: return@TextButton
                    onSave(
                        CustomReminderRequest(
                            id = existing?.id,
                            title = title,
                            category = category,
                            reminderDate = date,
                            reminderTimeMinutes = selectedTimeMinutes,
                            repeatType = repeatType,
                            importance = importance,
                            notes = notes
                        )
                    )
                }
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}
