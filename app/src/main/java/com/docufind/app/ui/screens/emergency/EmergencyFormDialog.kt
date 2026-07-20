package com.docufind.app.ui.screens.emergency

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.EmergencyContact
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.ui.components.form.DocuFindFamilyMemberPicker
import com.docufind.app.ui.components.form.DocuFindFormScaffold
import com.docufind.app.ui.components.form.DocuFindFormSection
import com.docufind.app.ui.components.form.DocuFindRelationPicker
import com.docufind.app.ui.util.FormValidation

@Composable
fun EmergencyFormDialog(
    contact: EmergencyContact?,
    familyMembers: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        alternatePhone: String?,
        email: String?,
        relation: FamilyRelation,
        linkedFamilyMemberId: String?,
        notes: String?,
        isPrimary: Boolean
    ) -> Unit
) {
    var name by remember(contact) { mutableStateOf(contact?.name.orEmpty()) }
    var phone by remember(contact) { mutableStateOf(contact?.phone.orEmpty()) }
    var alternatePhone by remember(contact) { mutableStateOf(contact?.alternatePhone.orEmpty()) }
    var email by remember(contact) { mutableStateOf(contact?.email.orEmpty()) }
    var relation by remember(contact) { mutableStateOf(FamilyRelation.fromStored(contact?.relationship)) }
    var linkedMemberId by remember(contact) { mutableStateOf(contact?.linkedFamilyMemberId) }
    var notes by remember(contact) { mutableStateOf(contact?.notes.orEmpty()) }
    var isPrimary by remember(contact) { mutableStateOf(contact?.isPrimary ?: false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var altPhoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val nameRequiredMsg = stringResource(R.string.validation_name_required)
    val phoneRequiredMsg = stringResource(R.string.validation_phone_required)
    val phoneInvalidMsg = stringResource(R.string.validation_phone_invalid)
    val emailInvalidMsg = stringResource(R.string.validation_email_invalid)

    DocuFindFormScaffold(
        title = if (contact == null) {
            stringResource(R.string.emergency_add_title)
        } else {
            stringResource(R.string.emergency_edit_title)
        },
        onBack = onDismiss,
        onSave = {
            nameError = if (FormValidation.nameError(name)) nameRequiredMsg else null
            phoneError = when {
                phone.trim().isEmpty() -> phoneRequiredMsg
                FormValidation.phoneError(phone, required = true) -> phoneInvalidMsg
                else -> null
            }
            altPhoneError = if (alternatePhone.isNotBlank() && FormValidation.phoneError(alternatePhone, required = true)) {
                phoneInvalidMsg
            } else null
            emailError = if (FormValidation.emailError(email)) emailInvalidMsg else null
            if (nameError == null && phoneError == null && altPhoneError == null && emailError == null) {
                onSave(
                    name.trim(),
                    FormValidation.normalizePhone(phone),
                    alternatePhone.trim().takeIf { it.isNotEmpty() }?.let(FormValidation::normalizePhone),
                    email.trim().takeIf { it.isNotEmpty() },
                    relation,
                    linkedMemberId,
                    notes.trim().takeIf { it.isNotEmpty() },
                    isPrimary
                )
            }
        }
    ) {
        DocuFindFormSection(
            title = stringResource(R.string.form_section_identity),
            icon = Icons.Default.Person
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text(stringResource(R.string.field_name)) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            DocuFindRelationPicker(
                selected = relation,
                onSelected = { relation = it },
                emergencyContext = true
            )
        }

        DocuFindFormSection(
            title = stringResource(R.string.form_section_contact),
            icon = Icons.Default.ContactPhone
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                },
                label = { Text(stringResource(R.string.field_phone_number)) },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = alternatePhone,
                onValueChange = {
                    alternatePhone = it
                    altPhoneError = null
                },
                label = { Text(stringResource(R.string.field_alt_phone_optional)) },
                isError = altPhoneError != null,
                supportingText = altPhoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text(stringResource(R.string.field_email_optional)) },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        DocuFindFormSection(
            title = stringResource(R.string.form_section_link),
            icon = Icons.Default.Link
        ) {
            DocuFindFamilyMemberPicker(
                members = familyMembers,
                selectedId = linkedMemberId,
                onSelected = { linkedMemberId = it },
                label = stringResource(R.string.field_linked_family_member_optional)
            )
        }

        DocuFindFormSection(
            title = stringResource(R.string.form_section_notes),
            icon = Icons.AutoMirrored.Filled.Notes
        ) {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.field_notes)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.field_primary_contact),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.field_primary_contact_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
