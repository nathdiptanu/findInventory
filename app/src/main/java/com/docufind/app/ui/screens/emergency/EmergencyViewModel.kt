package com.docufind.app.ui.screens.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.EmergencyContact
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.domain.repository.EmergencyContactRepository
import com.docufind.app.domain.repository.FamilyMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmergencyListUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val familyMemberOptions: List<Pair<String, String>> = emptyList(),
    val searchQuery: String = "",
    val showForm: Boolean = false,
    val editingContact: EmergencyContact? = null,
    val pendingDeleteId: String? = null,
    val errorMessage: String? = null
) {
    val filteredContacts: List<EmergencyContact>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return contacts
            return contacts.filter { contact ->
                contact.name.lowercase().contains(query) ||
                    contact.phone.contains(query) ||
                    contact.email.orEmpty().lowercase().contains(query) ||
                    contact.relationship.orEmpty().lowercase().contains(query)
            }
        }
}

@HiltViewModel
class EmergencyListViewModel @Inject constructor(
    private val repository: EmergencyContactRepository,
    familyMemberRepository: FamilyMemberRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyListUiState())
    val uiState: StateFlow<EmergencyListUiState> = combine(
        repository.observeAll(),
        familyMemberRepository.observeAll(),
        _uiState
    ) { contacts, members, state ->
        state.copy(
            contacts = contacts,
            familyMemberOptions = members.map { it.id to it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmergencyListUiState())

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun showAddForm() = _uiState.update { it.copy(showForm = true, editingContact = null) }

    fun showEditForm(contact: EmergencyContact) =
        _uiState.update { it.copy(showForm = true, editingContact = contact) }

    fun dismissForm() = _uiState.update { it.copy(showForm = false, editingContact = null) }

    fun requestDelete(id: String) = _uiState.update { it.copy(pendingDeleteId = id) }

    fun dismissDelete() = _uiState.update { it.copy(pendingDeleteId = null) }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _uiState.update { it.copy(pendingDeleteId = null) }
        }
    }

    fun saveContact(
        name: String,
        phone: String,
        alternatePhone: String?,
        email: String?,
        relation: FamilyRelation,
        linkedFamilyMemberId: String?,
        notes: String?,
        isPrimary: Boolean
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.editingContact
            val now = System.currentTimeMillis()
            val contact = EmergencyContact(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                alternatePhone = alternatePhone?.trim()?.takeIf { it.isNotEmpty() },
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                relationship = relation.name,
                linkedFamilyMemberId = linkedFamilyMemberId,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                isPrimary = isPrimary,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repository.save(contact).fold(
                onSuccess = { dismissForm() },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: context.getString(R.string.error_save_failed))
                    }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
