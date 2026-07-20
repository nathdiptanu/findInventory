package com.docufind.app.ui.screens.pets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.domain.model.pets.PetGender
import com.docufind.app.domain.model.pets.PetType
import com.docufind.app.ui.components.DocuFindDateField
import com.docufind.app.ui.components.form.DocuFindOptionPicker

@Composable
fun PetFormDialog(
    pet: Pet?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        petType: PetType,
        breed: String?,
        gender: PetGender,
        birthDate: Long?,
        weight: String?,
        color: String?,
        microchipId: String?,
        vetName: String?,
        vetPhone: String?,
        notes: String?,
        photoUri: String?,
        removePhoto: Boolean
    ) -> Unit
) {
    var name by remember(pet) { mutableStateOf(pet?.name.orEmpty()) }
    var petType by remember(pet) { mutableStateOf(PetType.fromStored(pet?.petType)) }
    var breed by remember(pet) { mutableStateOf(pet?.breed.orEmpty()) }
    var gender by remember(pet) { mutableStateOf(PetGender.fromStored(pet?.gender)) }
    var birthDate by remember(pet) { mutableStateOf(pet?.birthDate) }
    var weight by remember(pet) { mutableStateOf(pet?.weight.orEmpty()) }
    var color by remember(pet) { mutableStateOf(pet?.color.orEmpty()) }
    var microchipId by remember(pet) { mutableStateOf(pet?.microchipId.orEmpty()) }
    var vetName by remember(pet) { mutableStateOf(pet?.vetName.orEmpty()) }
    var vetPhone by remember(pet) { mutableStateOf(pet?.vetPhone.orEmpty()) }
    var notes by remember(pet) { mutableStateOf(pet?.notes.orEmpty()) }
    var photoUri by remember(pet) { mutableStateOf<String?>(null) }
    var removePhoto by remember(pet) { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoUri = it.toString()
            removePhoto = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (pet == null) stringResource(R.string.pet_add_title)
                else stringResource(R.string.pet_edit_title)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_pet_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                DocuFindOptionPicker(
                    label = stringResource(R.string.field_pet_type),
                    options = PetType.all.map { it.displayName },
                    selected = petType.displayName,
                    onSelected = { label ->
                        petType = PetType.all.first { it.displayName == label }
                    }
                )
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text(stringResource(R.string.field_breed)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                DocuFindOptionPicker(
                    label = stringResource(R.string.field_gender),
                    options = PetGender.all.map { it.displayName },
                    selected = gender.displayName,
                    onSelected = { label ->
                        gender = PetGender.all.first { it.displayName == label }
                    }
                )
                DocuFindDateField(
                    label = stringResource(R.string.field_dob_adoption),
                    epochMillis = birthDate,
                    onDateSelected = { birthDate = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.field_weight)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(stringResource(R.string.field_color)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = microchipId,
                    onValueChange = { microchipId = it },
                    label = { Text(stringResource(R.string.field_microchip)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = vetName,
                    onValueChange = { vetName = it },
                    label = { Text(stringResource(R.string.field_vet_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = vetPhone,
                    onValueChange = { vetPhone = it },
                    label = { Text(stringResource(R.string.field_vet_phone)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                TextButton(onClick = { photoPicker.launch("image/*") }) {
                    Text(
                        if (photoUri != null || (!removePhoto && pet?.photoPath != null)) {
                            stringResource(R.string.photo_change)
                        } else {
                            stringResource(R.string.photo_add_optional)
                        }
                    )
                }
                if (photoUri != null || (!removePhoto && pet?.photoPath != null)) {
                    TextButton(onClick = {
                        photoUri = null
                        removePhoto = true
                    }) {
                        Text(stringResource(R.string.photo_remove))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name,
                    petType,
                    breed,
                    gender,
                    birthDate,
                    weight,
                    color,
                    microchipId,
                    vetName,
                    vetPhone,
                    notes,
                    photoUri,
                    removePhoto
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

