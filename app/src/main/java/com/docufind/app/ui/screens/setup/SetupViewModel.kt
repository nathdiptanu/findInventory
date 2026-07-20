package com.docufind.app.ui.screens.setup

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.R
import com.docufind.app.domain.repository.PreferencesRepository
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.auth.BiometricAuthManager
import com.docufind.app.security.keystore.DatabaseKeyManager
import com.docufind.app.security.keystore.KeystoreManager
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.security.settings.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinUiState(
    val enteredLength: Int = 0,
    val isConfirmStep: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val securityPreferences: SecurityPreferences,
    private val keystoreManager: KeystoreManager,
    private val databaseKeyManager: DatabaseKeyManager,
    private val pinManager: PinManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val vaultSessionManager: VaultSessionManager
) : ViewModel() {

    private val _pinUiState = MutableStateFlow(PinUiState())
    val pinUiState: StateFlow<PinUiState> = _pinUiState.asStateFlow()

    private var firstPin = StringBuilder()
    private var confirmPin = StringBuilder()

    init {
        runCatching {
            keystoreManager.getOrCreateMasterKey()
            databaseKeyManager.getDatabasePassphrase()
        }
    }

    fun onPinDigit(digit: Int) {
        val builder = if (_pinUiState.value.isConfirmStep) confirmPin else firstPin
        if (builder.length >= PIN_LENGTH) return

        builder.append(digit)
        _pinUiState.update {
            it.copy(enteredLength = builder.length, errorMessage = null)
        }

        if (builder.length == PIN_LENGTH) {
            if (!_pinUiState.value.isConfirmStep) {
                _pinUiState.update { it.copy(isConfirmStep = true, enteredLength = 0) }
            } else {
                if (firstPin.toString() == confirmPin.toString()) {
                    viewModelScope.launch {
                        pinManager.setPin(firstPin.toString().toCharArray())
                        preferencesRepository.setPinConfigured(true)
                        _pinUiState.update { it.copy(completed = true) }
                    }
                } else {
                    firstPin.clear()
                    confirmPin.clear()
                    _pinUiState.update {
                        it.copy(
                            isConfirmStep = false,
                            enteredLength = 0,
                            errorMessage = context.getString(R.string.pin_mismatch)
                        )
                    }
                }
            }
        }
    }

    fun onPinDelete() {
        val builder = if (_pinUiState.value.isConfirmStep) confirmPin else firstPin
        if (builder.isNotEmpty()) {
            builder.deleteCharAt(builder.lastIndex)
            _pinUiState.update { it.copy(enteredLength = builder.length) }
        }
    }

    fun resetPinCompleted() {
        _pinUiState.update { it.copy(completed = false) }
    }

    fun isBiometricAvailable(): Boolean = biometricAuthManager.isBiometricAvailable()

    suspend fun enrollBiometric(activity: FragmentActivity): AuthResult {
        if (!biometricAuthManager.isBiometricAvailable()) {
            return AuthResult.Error(context.getString(R.string.biometric_unavailable))
        }
        return biometricAuthManager.authenticateForEnrollment(activity)
    }

    fun enableBiometric() {
        viewModelScope.launch {
            securityPreferences.setBiometricEnabled(true)
            preferencesRepository.setBiometricEnabled(true)
        }
    }

    fun skipBiometric() {
        viewModelScope.launch {
            securityPreferences.setBiometricEnabled(false)
            preferencesRepository.setBiometricEnabled(false)
        }
    }

    fun finishSecuritySetup(unlockVault: Boolean = true, onDone: () -> Unit) {
        viewModelScope.launch {
            if (unlockVault) {
                vaultSessionManager.unlock()
            }
            preferencesRepository.scheduleHowToUseAutoExpand()
            onDone()
        }
    }

    companion object {
        private const val PIN_LENGTH = 6
    }
}
