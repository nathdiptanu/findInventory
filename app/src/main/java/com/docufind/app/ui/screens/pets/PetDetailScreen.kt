package com.docufind.app.ui.screens.pets

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.BuildConfig
import com.docufind.app.R
import com.docufind.app.domain.model.pets.PetGender
import com.docufind.app.domain.model.pets.PetRecordType
import com.docufind.app.domain.model.pets.PetType
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.module.ModuleFilterChips
import com.docufind.app.ui.components.profile.ProfileEmptyState
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.theme.DocuFindPetTeal
import com.docufind.app.ui.util.formatDisplayDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PetDetailViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pet = uiState.pet
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.showDeletePetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletePetDialog,
            title = { Text(stringResource(R.string.delete_pet_title)) },
            text = { Text(stringResource(R.string.delete_pet_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePet(onDeleted) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeletePetDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    uiState.showDeleteRecordDialog?.let { recordId ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteRecordDialog,
            title = { Text(stringResource(R.string.delete_record_title)) },
            text = { Text(stringResource(R.string.delete_record_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord(recordId) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteRecordDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showPetForm && pet != null) {
        PetFormDialog(
            pet = pet,
            onDismiss = viewModel::dismissPetForm,
            onSave = { name, type, breed, gender, birth, weight, color, chip, vet, vetPhone, notes, photo, removePhoto ->
                viewModel.savePet(
                    name,
                    type,
                    breed,
                    gender,
                    birth,
                    weight,
                    color,
                    chip,
                    vet,
                    vetPhone,
                    notes,
                    photo,
                    removePhoto
                )
            }
        )
    }

    if (uiState.showRecordForm) {
        PetRecordFormDialog(
            record = uiState.editingRecord,
            onDismiss = viewModel::dismissRecordForm,
            onSave = { title, type, vaccine, date, nextDue, clinic, reminder, notes, attachment ->
                viewModel.saveRecord(title, type, vaccine, date, nextDue, clinic, reminder, notes, attachment)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(pet?.name ?: stringResource(R.string.pets_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showPetForm) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = viewModel::showDeletePetDialog) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddRecordForm,
                containerColor = DocuFindPetTeal
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        pet?.let { p ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailLine(stringResource(R.string.field_pet_type), PetType.fromStored(p.petType).displayName)
                            p.breed?.let { DetailLine(stringResource(R.string.field_breed), it) }
                            DetailLine(stringResource(R.string.field_gender), PetGender.fromStored(p.gender).displayName)
                            p.birthDate?.let { DetailLine(stringResource(R.string.field_dob_adoption), formatDisplayDate(it)) }
                            p.weight?.let { DetailLine(stringResource(R.string.field_weight), it) }
                            p.color?.let { DetailLine(stringResource(R.string.field_color), it) }
                            p.microchipId?.let { DetailLine(stringResource(R.string.field_microchip), it) }
                            p.vetName?.let { DetailLine(stringResource(R.string.field_vet_name), it) }
                            p.vetPhone?.let { DetailLine(stringResource(R.string.field_vet_phone), it) }
                            p.notes?.let { DetailLine(stringResource(R.string.field_notes), it) }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.pet_records_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DocuFindPetTeal
                    )
                }
                item {
                    DocuFindSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchChange,
                        placeholder = stringResource(R.string.pet_records_search_hint),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    ModuleFilterChips(
                        chips = uiState.filterChips,
                        selected = uiState.selectedFilter,
                        onSelected = viewModel::onFilterSelected
                    )
                }
                if (uiState.filteredRecords.isEmpty()) {
                    item {
                        ProfileEmptyState(
                            title = stringResource(R.string.empty_title),
                            message = stringResource(R.string.pet_records_empty_message)
                        )
                    }
                } else {
                    items(uiState.filteredRecords, key = { it.id }) { record ->
                        val type = PetRecordType.fromStored(record.recordType)
                        DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(record.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    type.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                record.recordDate?.let {
                                    Text(formatDisplayDate(it), style = MaterialTheme.typography.bodySmall)
                                }
                                record.vaccineName?.let {
                                    Text("${stringResource(R.string.field_vaccine_name)}: $it")
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    IconButton(onClick = { viewModel.showEditRecordForm(record) }) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                    IconButton(onClick = { viewModel.showDeleteRecordDialog(record.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                    if (record.attachmentPath != null) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                viewModel.decryptAttachment(record).onSuccess { file ->
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                        file
                                                    )
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, record.attachmentMimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, null))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Visibility, contentDescription = stringResource(R.string.preview))
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                viewModel.decryptAttachment(record).onSuccess { file ->
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                        file
                                                    )
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        setType(record.attachmentMimeType)
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, null))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
