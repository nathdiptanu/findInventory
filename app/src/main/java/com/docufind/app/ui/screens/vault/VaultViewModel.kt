package com.docufind.app.ui.screens.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.AuthPurpose
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.auth.BiometricAuthManager
import com.docufind.app.security.auth.FriendlyAuthMessages
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.security.settings.SecurityPreferences
import com.docufind.app.insights.ActivityInsightsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isLocked: Boolean = true,
    val pinConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val authError: String? = null,
    val isAuthenticating: Boolean = false
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultSessionManager: VaultSessionManager,
    private val authGate: AuthGate,
    private val pinManager: PinManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val activityInsightsTracker: ActivityInsightsTracker,
    securityPreferences: SecurityPreferences
) : ViewModel() {

    private val _authError = MutableStateFlow<String?>(null)
    private val _isAuthenticating = MutableStateFlow(false)
    private val _pinConfigured = MutableStateFlow(pinManager.hasPinConfigured())

    val authError: StateFlow<String?> = _authError
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating

    val uiState: StateFlow<VaultUiState> = combine(
        vaultSessionManager.isUnlocked,
        securityPreferences.preferences,
        _authError,
        _isAuthenticating,
        _pinConfigured
    ) { unlocked, settings, error, authenticating, pinConfigured ->
        VaultUiState(
            isLocked = !unlocked,
            pinConfigured = pinConfigured,
            biometricEnabled = settings.biometricEnabled,
            biometricAvailable = biometricAuthManager.isBiometricAvailable(),
            authError = error,
            isAuthenticating = authenticating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(
            pinConfigured = pinManager.hasPinConfigured(),
            biometricAvailable = biometricAuthManager.isBiometricAvailable()
        )
    )

    fun refreshPinConfigured() {
        _pinConfigured.value = pinManager.hasPinConfigured()
    }

    fun onSecuritySetupComplete() {
        _pinConfigured.value = true
        _authError.value = null
    }

    fun unlockWithPin(activity: FragmentActivity, pin: CharArray) {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _authError.value = null
            _isAuthenticating.value = true
            try {
                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT, pin)) {
                    is AuthResult.Success -> {
                        _authError.value = null
                        activityInsightsTracker.trackVaultOpened()
                    }
                    is AuthResult.Error -> _authError.value = FriendlyAuthMessages.forUnlock(result.message)
                    AuthResult.Cancelled -> Unit
                    AuthResult.Failed -> _authError.value = "Could not unlock. Try your PIN."
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _authError.value = null
            _isAuthenticating.value = true
            try {
                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT)) {
                    is AuthResult.Success -> {
                        _authError.value = null
                        activityInsightsTracker.trackVaultOpened()
                    }
                    is AuthResult.Error -> _authError.value = FriendlyAuthMessages.forUnlock(result.message)
                    AuthResult.Cancelled -> Unit
                    AuthResult.Failed -> _authError.value = "Biometric did not match. Use your PIN."
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun resetPinWithBiometric(activity: FragmentActivity) {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _authError.value = null
            _isAuthenticating.value = true
            try {
                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT)) {
                    is AuthResult.Success -> {
                        pinManager.clearPinForReset()
                        authGate.lockVault()
                        _pinConfigured.value = false
                        _authError.value = null
                    }
                    is AuthResult.Error -> _authError.value = FriendlyAuthMessages.forUnlock(result.message)
                    AuthResult.Cancelled -> Unit
                    AuthResult.Failed -> _authError.value = "Biometric didn't match. Use your PIN."
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun resetPinWithoutBiometric() {
        pinManager.clearPinForReset()
        authGate.lockVault()
        _pinConfigured.value = false
        _authError.value = null
    }

    fun lockVault() {
        authGate.lockVault()
    }

    fun clearAuthError() {
        _authError.value = null
    }

}
