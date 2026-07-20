package com.docufind.app.ui.screens.pets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.PetRecord
import com.docufind.app.domain.model.pets.PetRecordType
import com.docufind.app.ui.components.DocuFindDateField
import com.docufind.app.ui.components.form.DocuFindOptionPicker

@Composable
fun PetRecordFormDialog(
    record: PetRecord?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        recordType: PetRecordType,
        vaccineName: String?,
        recordDate: Long?,
        nextDueDate: Long?,
        vetClinic: String?,
        reminderEnabled: Boolean,
        notes: String?,
        attachmentUri: String?
    ) -> Unit
) {
    var title by remember(record) { mutableStateOf(record?.title.orEmpty()) }
    var recordType by remember(record) { mutableStateOf(PetRecordType.fromStored(record?.recordType)) }
    var vaccineName by remember(record) { mutableStateOf(record?.vaccineName.orEmpty()) }
    var recordDate by remember(record) { mutableStateOf(record?.recordDate) }
    var nextDueDate by remember(record) { mutableStateOf(record?.nextDueDate) }
    var vetClinic by remember(record) { mutableStateOf(record?.vetClinic.orEmpty()) }
    var reminderEnabled by remember(record) { mutableStateOf(record?.reminderEnabled ?: false) }
    var notes by remember(record) { mutableStateOf(record?.notes.orEmpty()) }
    var attachmentUri by remember(record) { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> attachmentUri = uri?.toString() }

    val isVaccination = recordType == PetRecordType.VACCINATION

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (record == null) stringResource(R.string.pet_record_add_title)
                else stringResource(R.string.pet_record_edit_title)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DocuFindOptionPicker(
                    label = stringResource(R.string.field_record_type),
                    options = PetRecordType.all.map { it.displayName },
                    selected = recordType.displayName,
                    onSelected = { label ->
                        recordType = PetRecordType.all.first { it.displayName == label }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                if (isVaccination) {
                    OutlinedTextField(
                        value = vaccineName,
                        onValueChange = { vaccineName = it },
                        label = { Text(stringResource(R.string.field_vaccine_name)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                DocuFindDateField(
                    label = if (isVaccination) stringResource(R.string.field_date_given) else stringResource(R.string.field_date),
                    epochMillis = recordDate,
                    onDateSelected = { recordDate = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (isVaccination) {
                    DocuFindDateField(
                        label = stringResource(R.string.field_next_due),
                        epochMillis = nextDueDate,
                        onDateSelected = { nextDueDate = it },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = vetClinic,
                        onValueChange = { vetClinic = it },
                        label = { Text(stringResource(R.string.field_vet_clinic)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Text(stringResource(R.string.field_reminder_enabled))
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                TextButton(onClick = { filePicker.launch(arrayOf("image/*", "application/pdf")) }) {
                    Text(
                        if (attachmentUri != null || record?.attachmentPath != null) {
                            stringResource(R.string.attachment_change)
                        } else {
                            stringResource(R.string.attachment_upload)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    title,
                    recordType,
                    vaccineName.takeIf { isVaccination && it.isNotBlank() },
                    recordDate,
                    nextDueDate.takeIf { isVaccination },
                    vetClinic.takeIf { isVaccination && it.isNotBlank() },
                    reminderEnabled && isVaccination,
                    notes,
                    attachmentUri
                )
            }) {
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
