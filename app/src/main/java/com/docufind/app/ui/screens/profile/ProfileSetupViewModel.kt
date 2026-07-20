package com.docufind.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val nameError: String? = null
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onMobileChange(value: String) {
        _uiState.update { it.copy(mobile = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun saveProfile(onComplete: () -> Unit) {
        val trimmed = _uiState.value.name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            preferencesRepository.saveUserProfile(
                name = trimmed,
                mobile = _uiState.value.mobile.trim().ifBlank { null },
                email = _uiState.value.email.trim().ifBlank { null }
            )
            onComplete()
        }
    }
}
