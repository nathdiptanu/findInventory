package com.docufind.app.ui.navigation



import androidx.fragment.app.FragmentActivity

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.docufind.app.reminder.ReminderPendingNavigation

import com.docufind.app.security.auth.AuthGate

import com.docufind.app.security.auth.AuthPurpose

import com.docufind.app.security.auth.AuthResult

import com.docufind.app.security.auth.BiometricAuthManager

import com.docufind.app.security.auth.FriendlyAuthMessages

import com.docufind.app.security.pin.PinManager

import com.docufind.app.security.session.VaultSessionManager

import com.docufind.app.security.settings.SecurityPreferences

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.SharingStarted

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch



data class UnlockPromptState(

    val visible: Boolean = false,

    val navigateToVaultForSetup: Boolean = false

)



@HiltViewModel

class MainNavigationViewModel @Inject constructor(

    private val reminderPendingNavigation: ReminderPendingNavigation,

    private val vaultSessionManager: VaultSessionManager,

    private val authGate: AuthGate,

    private val pinManager: PinManager,

    private val biometricAuthManager: BiometricAuthManager,

    securityPreferences: SecurityPreferences

) : ViewModel() {



    private val _unlockPrompt = MutableStateFlow(UnlockPromptState())

    val unlockPrompt: StateFlow<UnlockPromptState> = _unlockPrompt.asStateFlow()



    private val _unlockAuthError = MutableStateFlow<String?>(null)

    val unlockAuthError: StateFlow<String?> = _unlockAuthError.asStateFlow()



    private val _isUnlockAuthenticating = MutableStateFlow(false)

    val isUnlockAuthenticating: StateFlow<Boolean> = _isUnlockAuthenticating.asStateFlow()



    val unlockUiState = combine(

        securityPreferences.preferences,

        _unlockAuthError,

        _isUnlockAuthenticating

    ) { settings, error, authenticating ->

        Triple(settings.biometricEnabled, error, authenticating)

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(false, null, false))



    val biometricAvailable: Boolean get() = biometricAuthManager.isBiometricAvailable()



    private val _navigateToRecord = MutableStateFlow<String?>(null)

    val navigateToRecord: StateFlow<String?> = _navigateToRecord.asStateFlow()



    private val _navigateToPet = MutableStateFlow<String?>(null)

    val navigateToPet: StateFlow<String?> = _navigateToPet.asStateFlow()



    private val _navigateToSearch = MutableStateFlow(false)

    val navigateToSearch: StateFlow<Boolean> = _navigateToSearch.asStateFlow()



    private val _navigateToVault = MutableStateFlow(false)

    val navigateToVault: StateFlow<Boolean> = _navigateToVault.asStateFlow()



    private val _pendingSearchQuery = MutableStateFlow<String?>(null)

    val pendingSearchQuery: StateFlow<String?> = _pendingSearchQuery.asStateFlow()



    private var pendingRecordId: String? = null

    private var pendingPetId: String? = null

    private var pendingOpenSearch = false

    private var pendingSearchAfterUnlock: String? = null



    fun consumePendingNavigation() {

        val pending = reminderPendingNavigation.consume() ?: return

        if (pending.requiresUnlock && !vaultSessionManager.isUnlocked.value) {

            if (!pinManager.hasPinConfigured()) {

                reminderPendingNavigation.setPending(pending)

                _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)

                _navigateToVault.value = true

            } else {

                reminderPendingNavigation.setPending(pending)

                _unlockPrompt.value = UnlockPromptState(visible = true)

            }

            return

        }

        routePending(pending)

    }



    fun openPet(petId: String) {
        if (petId.isBlank()) return
        if (!pinManager.hasPinConfigured()) {
            pendingPetId = petId
            _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)
            _navigateToVault.value = true
            return
        }
        if (vaultSessionManager.isUnlocked.value) {
            _navigateToPet.value = petId
        } else {
            pendingPetId = petId
            _unlockAuthError.value = null
            _unlockPrompt.value = UnlockPromptState(visible = true)
        }
    }

    fun openRecord(recordId: String) {

        if (recordId.isBlank()) return

        if (!pinManager.hasPinConfigured()) {

            pendingRecordId = recordId

            _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)

            _navigateToVault.value = true

            return

        }

        if (vaultSessionManager.isUnlocked.value) {

            _navigateToRecord.value = recordId

        } else {

            pendingRecordId = recordId

            _unlockAuthError.value = null

            _unlockPrompt.value = UnlockPromptState(visible = true)

        }

    }



    fun openSearch(query: String) {

        val trimmed = query.trim()

        if (!pinManager.hasPinConfigured()) {

            pendingOpenSearch = true

            pendingSearchAfterUnlock = trimmed.ifBlank { null }

            _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)

            _navigateToVault.value = true

            return

        }

        if (vaultSessionManager.isUnlocked.value) {

            _pendingSearchQuery.value = trimmed.ifBlank { null }

            _navigateToSearch.value = true

        } else {

            pendingOpenSearch = true

            pendingSearchAfterUnlock = trimmed.ifBlank { null }

            _unlockAuthError.value = null

            _unlockPrompt.value = UnlockPromptState(visible = true)

        }

    }



    fun onSearchNavigationHandled() {

        _navigateToSearch.value = false

    }



    fun onVaultNavigationHandled() {

        _navigateToVault.value = false

    }



    fun consumeSearchQuery(): String? = _pendingSearchQuery.value.also { _pendingSearchQuery.value = null }



    fun unlockWithPin(activity: FragmentActivity, pin: CharArray) {

        if (_isUnlockAuthenticating.value) return

        viewModelScope.launch {

            _unlockAuthError.value = null

            _isUnlockAuthenticating.value = true

            try {

                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT, pin)) {

                    is AuthResult.Success -> onUnlockSuccess()

                    is AuthResult.Error -> _unlockAuthError.value = FriendlyAuthMessages.forUnlock(result.message)

                    AuthResult.Cancelled -> Unit

                    AuthResult.Failed -> _unlockAuthError.value = "That PIN didn't match. Please try again."

                }

            } finally {

                _isUnlockAuthenticating.value = false

            }

        }

    }



    fun unlockWithBiometric(activity: FragmentActivity) {

        if (_isUnlockAuthenticating.value) return

        viewModelScope.launch {

            _unlockAuthError.value = null

            _isUnlockAuthenticating.value = true

            try {

                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT)) {

                    is AuthResult.Success -> onUnlockSuccess()

                    is AuthResult.Error -> _unlockAuthError.value = FriendlyAuthMessages.forUnlock(result.message)

                    AuthResult.Cancelled -> Unit

                    AuthResult.Failed -> _unlockAuthError.value = "Biometric didn't match. Use your PIN instead."

                }

            } finally {

                _isUnlockAuthenticating.value = false

            }

        }

    }



    private fun onUnlockSuccess() {

        _unlockPrompt.value = UnlockPromptState()

        _unlockAuthError.value = null

        reminderPendingNavigation.consume()?.let { routePending(it) }

        pendingRecordId?.let { id ->

            pendingRecordId = null

            _navigateToRecord.value = id

        }

        pendingPetId?.let { id ->

            pendingPetId = null

            _navigateToPet.value = id

        }

        if (pendingSearchAfterUnlock != null) {

            val query = pendingSearchAfterUnlock

            pendingSearchAfterUnlock = null

            pendingOpenSearch = false

            _pendingSearchQuery.value = query

            _navigateToSearch.value = true

        } else if (pendingOpenSearch) {

            pendingOpenSearch = false

            _pendingSearchQuery.value = null

            _navigateToSearch.value = true

        }

    }



    fun dismissUnlockPrompt() {

        _unlockPrompt.value = UnlockPromptState()

        _unlockAuthError.value = null

        pendingRecordId = null

        pendingPetId = null

        pendingSearchAfterUnlock = null

        pendingOpenSearch = false

        reminderPendingNavigation.consume()

    }

    fun resetPinWithBiometric(activity: FragmentActivity) {
        if (_isUnlockAuthenticating.value) return
        viewModelScope.launch {
            _unlockAuthError.value = null
            _isUnlockAuthenticating.value = true
            try {
                when (val result = authGate.authenticateForPurpose(activity, AuthPurpose.VAULT)) {
                    is AuthResult.Success -> {
                        pinManager.clearPinForReset()
                        authGate.lockVault()
                        dismissUnlockPrompt()
                        _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)
                        _navigateToVault.value = true
                    }
                    is AuthResult.Error -> _unlockAuthError.value = FriendlyAuthMessages.forUnlock(result.message)
                    AuthResult.Cancelled -> Unit
                    AuthResult.Failed -> _unlockAuthError.value = "Biometric didn't match. Use your PIN instead."
                }
            } finally {
                _isUnlockAuthenticating.value = false
            }
        }
    }

    fun resetPinWithoutBiometric() {
        pinManager.clearPinForReset()
        authGate.lockVault()
        dismissUnlockPrompt()
        _unlockPrompt.value = UnlockPromptState(navigateToVaultForSetup = true)
        _navigateToVault.value = true
    }



    fun onRecordNavigationHandled() {

        _navigateToRecord.value = null

    }



    fun onPetNavigationHandled() {

        _navigateToPet.value = null

    }



    private fun routePending(pending: com.docufind.app.reminder.PendingReminderNavigation) {

        when {

            pending.linkedRecordId != null -> _navigateToRecord.value = pending.linkedRecordId

            pending.linkedPetId != null -> _navigateToPet.value = pending.linkedPetId

        }

    }

}

