package com.docufind.app.ui.screens.pets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.data.local.db.entity.PetRecord
import com.docufind.app.data.local.storage.SecureAttachmentStorage
import com.docufind.app.domain.model.pets.PetGender
import com.docufind.app.domain.model.pets.PetRecordType
import com.docufind.app.domain.model.pets.PetType
import com.docufind.app.domain.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetListUiState(
    val pets: List<Pet> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val showForm: Boolean = false,
    val editingPet: Pet? = null,
    val errorMessage: String? = null
) {
    val filterChips: List<String> = listOf("All") + PetType.all.map { it.displayName }
    val filteredPets: List<Pet>
        get() {
            val query = searchQuery.trim().lowercase()
            return pets.filter { pet ->
                val matchesQuery = query.isEmpty() ||
                    pet.name.lowercase().contains(query) ||
                    pet.breed.orEmpty().lowercase().contains(query)
                val type = PetType.fromStored(pet.petType)
                val matchesFilter = selectedFilter == "All" || type.displayName == selectedFilter
                matchesQuery && matchesFilter
            }
        }
}

@HiltViewModel
class PetListViewModel @Inject constructor(
    private val repository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetListUiState())
    val uiState: StateFlow<PetListUiState> = combine(
        repository.observeAll(),
        _uiState
    ) { pets, state ->
        state.copy(pets = pets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetListUiState())

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onFilterSelected(filter: String) = _uiState.update { it.copy(selectedFilter = filter) }

    fun showAddForm() = _uiState.update { it.copy(showForm = true, editingPet = null) }

    fun dismissForm() = _uiState.update { it.copy(showForm = false, editingPet = null) }

    fun savePet(
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
        removePhoto: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Pet name is required") }
            return
        }
        viewModelScope.launch {
            val existing = _uiState.value.editingPet
            val now = System.currentTimeMillis()
            val pet = Pet(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                petType = petType.name,
                breed = breed?.trim()?.takeIf { it.isNotEmpty() },
                gender = gender.name,
                birthDate = birthDate,
                weight = weight?.trim()?.takeIf { it.isNotEmpty() },
                color = color?.trim()?.takeIf { it.isNotEmpty() },
                microchipId = microchipId?.trim()?.takeIf { it.isNotEmpty() },
                vetName = vetName?.trim()?.takeIf { it.isNotEmpty() },
                vetPhone = vetPhone?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                photoPath = existing?.photoPath,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repository.savePet(pet, photoUri, removePhoto).fold(
                onSuccess = {
                    dismissForm()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Save failed") }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

data class PetDetailUiState(
    val pet: Pet? = null,
    val records: List<PetRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val showPetForm: Boolean = false,
    val showRecordForm: Boolean = false,
    val editingRecord: PetRecord? = null,
    val showDeletePetDialog: Boolean = false,
    val showDeleteRecordDialog: String? = null,
    val errorMessage: String? = null
) {
    val filterChips: List<String> = PetRecordType.filterChips
    val filteredRecords: List<PetRecord>
        get() {
            val query = searchQuery.trim().lowercase()
            return records.filter { record ->
                val type = PetRecordType.fromStored(record.recordType)
                val matchesFilter = selectedFilter == "All" || type.displayName == selectedFilter
                val matchesQuery = query.isEmpty() ||
                    record.title.lowercase().contains(query) ||
                    record.vaccineName.orEmpty().lowercase().contains(query)
                matchesFilter && matchesQuery
            }
        }
}

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PetRepository,
    private val secureAttachmentStorage: SecureAttachmentStorage
) : ViewModel() {

    private val petId: String = savedStateHandle.get<String>("petId").orEmpty()
    private val _uiState = MutableStateFlow(PetDetailUiState())

    val uiState: StateFlow<PetDetailUiState> = combine(
        repository.observeById(petId),
        repository.observeRecords(petId),
        _uiState
    ) { pet, records, state ->
        state.copy(pet = pet, records = records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetDetailUiState())

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onFilterSelected(filter: String) = _uiState.update { it.copy(selectedFilter = filter) }

    fun showPetForm() = _uiState.update { it.copy(showPetForm = true) }

    fun dismissPetForm() = _uiState.update { it.copy(showPetForm = false) }

    fun showAddRecordForm() = _uiState.update { it.copy(showRecordForm = true, editingRecord = null) }

    fun showEditRecordForm(record: PetRecord) =
        _uiState.update { it.copy(showRecordForm = true, editingRecord = record) }

    fun dismissRecordForm() = _uiState.update { it.copy(showRecordForm = false, editingRecord = null) }

    fun showDeletePetDialog() = _uiState.update { it.copy(showDeletePetDialog = true) }

    fun dismissDeletePetDialog() = _uiState.update { it.copy(showDeletePetDialog = false) }

    fun showDeleteRecordDialog(recordId: String) =
        _uiState.update { it.copy(showDeleteRecordDialog = recordId) }

    fun dismissDeleteRecordDialog() = _uiState.update { it.copy(showDeleteRecordDialog = null) }

    fun savePet(
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
    ) {
        val existing = _uiState.value.pet ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val pet = existing.copy(
                name = name.trim(),
                petType = petType.name,
                breed = breed?.trim()?.takeIf { it.isNotEmpty() },
                gender = gender.name,
                birthDate = birthDate,
                weight = weight?.trim()?.takeIf { it.isNotEmpty() },
                color = color?.trim()?.takeIf { it.isNotEmpty() },
                microchipId = microchipId?.trim()?.takeIf { it.isNotEmpty() },
                vetName = vetName?.trim()?.takeIf { it.isNotEmpty() },
                vetPhone = vetPhone?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = now
            )
            repository.savePet(pet, photoUri, removePhoto).fold(
                onSuccess = { dismissPetForm() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Save failed") }
                }
            )
        }
    }

    fun saveRecord(
        title: String,
        recordType: PetRecordType,
        vaccineName: String?,
        recordDate: Long?,
        nextDueDate: Long?,
        vetClinic: String?,
        reminderEnabled: Boolean,
        notes: String?,
        attachmentUri: String?
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }
        viewModelScope.launch {
            val existing = _uiState.value.editingRecord
            val now = System.currentTimeMillis()
            val record = PetRecord(
                id = existing?.id ?: UUID.randomUUID().toString(),
                petId = petId,
                title = title.trim(),
                recordType = recordType.name,
                vaccineName = vaccineName?.trim()?.takeIf { it.isNotEmpty() },
                recordDate = recordDate,
                nextDueDate = nextDueDate,
                vetClinic = vetClinic?.trim()?.takeIf { it.isNotEmpty() },
                reminderEnabled = reminderEnabled,
                attachmentPath = existing?.attachmentPath,
                attachmentMimeType = existing?.attachmentMimeType,
                attachmentName = existing?.attachmentName,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repository.saveRecord(record, attachmentUri).fold(
                onSuccess = { dismissRecordForm() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Save failed") }
                }
            )
        }
    }

    fun deletePet(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePet(petId)
            onDeleted()
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            dismissDeleteRecordDialog()
        }
    }

    suspend fun decryptAttachment(record: PetRecord): Result<File> {
        val fileId = record.attachmentPath ?: return Result.failure(IllegalStateException("No file"))
        val mime = record.attachmentMimeType ?: "image/jpeg"
        return secureAttachmentStorage.decryptToCache(fileId, mime)
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
