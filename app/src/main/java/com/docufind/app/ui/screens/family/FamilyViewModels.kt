package com.docufind.app.ui.screens.family

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.FamilyMember
import com.docufind.app.domain.model.family.BloodGroup
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.data.local.storage.SecureAttachmentStorage
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.repository.FamilyMemberRepository
import com.docufind.app.domain.repository.VaultRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FamilyListUiState(
    val members: List<FamilyMember> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val showForm: Boolean = false,
    val editingMember: FamilyMember? = null,
    val avatarPreviewPath: String? = null,
    val errorMessage: String? = null
) {
    val filterChips: List<String> = listOf("All") + FamilyRelation.all.map { it.displayName }
    val filteredMembers: List<FamilyMember>
        get() {
            val query = searchQuery.trim().lowercase()
            return members.filter { member ->
                val matchesQuery = query.isEmpty() ||
                    member.name.lowercase().contains(query) ||
                    member.relationship.lowercase().contains(query) ||
                    member.phone.orEmpty().lowercase().contains(query)
                val matchesFilter = selectedFilter == "All" ||
                    FamilyRelation.fromStored(member.relationship).displayName == selectedFilter
                matchesQuery && matchesFilter
            }
        }
}

@HiltViewModel
class FamilyListViewModel @Inject constructor(
    private val repository: FamilyMemberRepository,
    private val secureAttachmentStorage: SecureAttachmentStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyListUiState())
    val uiState: StateFlow<FamilyListUiState> = combine(
        repository.observeAll(),
        _uiState
    ) { members, state ->
        state.copy(members = members)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyListUiState())

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onFilterSelected(filter: String) = _uiState.update { it.copy(selectedFilter = filter) }

    fun showAddForm() = _uiState.update {
        it.copy(showForm = true, editingMember = null, avatarPreviewPath = null)
    }

    fun showEditForm(member: FamilyMember) {
        _uiState.update { it.copy(showForm = true, editingMember = member, avatarPreviewPath = null) }
        viewModelScope.launch {
            val preview = member.avatarPath?.let { fileId ->
                secureAttachmentStorage.decryptToCache(fileId, "image/jpeg").getOrNull()?.absolutePath
            }
            _uiState.update { state ->
                if (state.editingMember?.id == member.id) state.copy(avatarPreviewPath = preview) else state
            }
        }
    }

    fun dismissForm() = _uiState.update {
        it.copy(showForm = false, editingMember = null, avatarPreviewPath = null)
    }

    fun saveMember(
        name: String,
        relation: FamilyRelation,
        dateOfBirth: Long?,
        bloodGroup: BloodGroup,
        phone: String?,
        email: String?,
        notes: String?,
        photoUri: String?,
        removePhoto: Boolean,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.validation_name_required)) }
            return
        }
        viewModelScope.launch {
            val existing = _uiState.value.editingMember
            val now = System.currentTimeMillis()
            val member = FamilyMember(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                relationship = relation.name,
                dateOfBirth = dateOfBirth,
                bloodGroup = bloodGroup.name,
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                avatarPath = existing?.avatarPath,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repository.save(member, photoUri, removePhoto).fold(
                onSuccess = {
                    dismissForm()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: context.getString(R.string.error_save_failed)) }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

@HiltViewModel
class FamilyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FamilyMemberRepository,
    private val vaultRecordRepository: VaultRecordRepository,
    private val secureAttachmentStorage: SecureAttachmentStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(FamilyDetailUiState())
    val uiState: StateFlow<FamilyDetailUiState> = combine(
        repository.observeById(memberId),
        vaultRecordRepository.observeFamilyMemberRecords(memberId),
        _uiState
    ) { member, documents, state ->
        state.copy(member = member, linkedDocuments = documents)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyDetailUiState())

    fun showEditForm() {
        viewModelScope.launch {
            val member = repository.observeById(memberId).first() ?: return@launch
            _uiState.update { it.copy(showForm = true, avatarPreviewPath = null) }
            val preview = member.avatarPath?.let { fileId ->
                secureAttachmentStorage.decryptToCache(fileId, "image/jpeg").getOrNull()?.absolutePath
            }
            _uiState.update { it.copy(avatarPreviewPath = preview) }
        }
    }

    fun dismissForm() = _uiState.update { it.copy(showForm = false, avatarPreviewPath = null) }

    fun showDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = true) }

    fun dismissDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun saveMember(
        name: String,
        relation: FamilyRelation,
        dateOfBirth: Long?,
        bloodGroup: BloodGroup,
        phone: String?,
        email: String?,
        notes: String?,
        photoUri: String?,
        removePhoto: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.observeById(memberId).first() ?: return@launch
            val now = System.currentTimeMillis()
            val member = existing.copy(
                name = name.trim(),
                relationship = relation.name,
                dateOfBirth = dateOfBirth,
                bloodGroup = bloodGroup.name,
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = now
            )
            repository.save(member, photoUri, removePhoto).fold(
                onSuccess = { dismissForm() },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: context.getString(R.string.error_save_failed))
                    }
                }
            )
        }
    }

    fun deleteMember(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(memberId)
            onDeleted()
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

data class FamilyDetailUiState(
    val member: FamilyMember? = null,
    val linkedDocuments: List<ModuleRecordItem> = emptyList(),
    val showForm: Boolean = false,
    val avatarPreviewPath: String? = null,
    val showDeleteDialog: Boolean = false,
    val errorMessage: String? = null
)
