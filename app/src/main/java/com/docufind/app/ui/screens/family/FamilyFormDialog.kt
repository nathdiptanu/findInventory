package com.docufind.app.ui.screens.family

import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.FamilyMember
import com.docufind.app.domain.model.family.BloodGroup
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.ui.components.DocuFindDateField
import com.docufind.app.ui.components.form.DocuFindBloodGroupSelector
import com.docufind.app.ui.components.form.DocuFindFormScaffold
import com.docufind.app.ui.components.form.DocuFindFormSection
import com.docufind.app.ui.components.form.DocuFindProfilePhotoPicker
import com.docufind.app.ui.components.form.DocuFindRelationPicker
import com.docufind.app.ui.util.FormValidation

@Composable
fun FamilyFormDialog(
    member: FamilyMember?,
    existingAvatarPreviewPath: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        relation: FamilyRelation,
        dateOfBirth: Long?,
        bloodGroup: BloodGroup,
        phone: String?,
        email: String?,
        notes: String?,
        photoUri: String?,
        removePhoto: Boolean
    ) -> Unit
) {
    var name by remember(member) { mutableStateOf(member?.name.orEmpty()) }
    var relation by remember(member) { mutableStateOf(FamilyRelation.fromStored(member?.relationship)) }
    var dateOfBirth by remember(member) { mutableStateOf(member?.dateOfBirth) }
    var bloodGroup by remember(member) { mutableStateOf(BloodGroup.fromStored(member?.bloodGroup)) }
    var phone by remember(member) { mutableStateOf(member?.phone.orEmpty()) }
    var email by remember(member) { mutableStateOf(member?.email.orEmpty()) }
    var notes by remember(member) { mutableStateOf(member?.notes.orEmpty()) }
    var photoUri by remember(member) { mutableStateOf<Uri?>(null) }
    var removePhoto by remember(member) { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var photoMessage by remember { mutableStateOf<String?>(null) }

    val hasExistingPhoto = !removePhoto && (photoUri != null || member?.avatarPath != null)

    val nameRequiredMsg = stringResource(R.string.validation_name_required)
    val phoneInvalidMsg = stringResource(R.string.validation_phone_invalid)
    val emailInvalidMsg = stringResource(R.string.validation_email_invalid)
    val photoPermissionDeniedMsg = stringResource(R.string.photo_permission_denied)

    DocuFindFormScaffold(
        title = if (member == null) {
            stringResource(R.string.family_add_title)
        } else {
            stringResource(R.string.family_edit_title)
        },
        onBack = onDismiss,
        onSave = {
            nameError = if (FormValidation.nameError(name)) nameRequiredMsg else null
            phoneError = if (FormValidation.phoneError(phone, required = false)) phoneInvalidMsg else null
            emailError = if (FormValidation.emailError(email)) emailInvalidMsg else null
            if (nameError == null && phoneError == null && emailError == null) {
                val normalizedPhone = phone.trim().takeIf { it.isNotEmpty() }?.let(FormValidation::normalizePhone)
                onSave(
                    name.trim(),
                    relation,
                    dateOfBirth,
                    bloodGroup,
                    normalizedPhone,
                    email.trim().takeIf { it.isNotEmpty() },
                    notes.trim().takeIf { it.isNotEmpty() },
                    photoUri?.toString(),
                    removePhoto
                )
            }
        }
    ) {
        DocuFindFormSection(
            title = stringResource(R.string.form_section_photo),
            icon = Icons.Default.PhotoCamera
        ) {
            DocuFindProfilePhotoPicker(
                photoUri = photoUri,
                hasExistingPhoto = hasExistingPhoto,
                existingPreviewPath = if (removePhoto) null else existingAvatarPreviewPath,
                onPhotoSelected = { uri ->
                    photoUri = uri
                    removePhoto = false
                    photoMessage = null
                },
                onPhotoRemoved = {
                    photoUri = null
                    removePhoto = true
                    photoMessage = null
                },
                onPermissionDenied = {
                    photoMessage = photoPermissionDeniedMsg
                }
            )
            photoMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        DocuFindFormSection(
            title = stringResource(R.string.form_section_profile),
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
                onSelected = { relation = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DocuFindDateField(
                label = stringResource(R.string.field_dob),
                epochMillis = dateOfBirth,
                onDateSelected = { dateOfBirth = it },
                allowClear = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            DocuFindBloodGroupSelector(
                selected = bloodGroup,
                onSelected = { bloodGroup = it }
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
                label = { Text(stringResource(R.string.field_phone_optional)) },
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
            Spacer(modifier = Modifier.height(16.dp))
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
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
