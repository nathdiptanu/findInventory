package com.docufind.app.ui.screens.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.repository.PreferencesRepository
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.auth.BiometricAuthManager
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.settings.AutoLockTimeout
import com.docufind.app.security.settings.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SecuritySettingsUiState(
    val pinConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.MIN_5,
    val allowScreenshots: Boolean = false
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val securityPreferences: SecurityPreferences,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SecuritySettingsUiState> = securityPreferences.preferences
        .map { settings ->
            SecuritySettingsUiState(
                pinConfigured = pinManager.hasPinConfigured(),
                biometricEnabled = settings.biometricEnabled,
                biometricAvailable = biometricAuthManager.isBiometricAvailable(),
                autoLockTimeout = settings.autoLockTimeout,
                allowScreenshots = settings.allowScreenshots
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecuritySettingsUiState())

    suspend fun enrollBiometric(activity: FragmentActivity): AuthResult =
        biometricAuthManager.authenticateForEnrollment(activity)

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setBiometricEnabled(enabled)
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            securityPreferences.setAutoLockTimeout(timeout)
        }
    }

    fun setAllowScreenshots(allowed: Boolean) {
        viewModelScope.launch {
            securityPreferences.setAllowScreenshots(allowed)
        }
    }
}
