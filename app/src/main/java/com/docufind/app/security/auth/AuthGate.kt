package com.docufind.app.security.auth

import androidx.fragment.app.FragmentActivity
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.pin.PinVerificationResult
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.security.settings.SecurityPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified authentication gate for vault, document view, export, and restore.
 */
@Singleton
class AuthGate @Inject constructor(
    private val pinManager: PinManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val vaultSessionManager: VaultSessionManager,
    private val securityPreferences: SecurityPreferences
) {
    suspend fun authenticateForPurpose(
        activity: FragmentActivity,
        purpose: AuthPurpose,
        pin: CharArray? = null
    ): AuthResult {
        // Only vault tab access may skip re-auth when the session is already unlocked.
        if (vaultSessionManager.isUnlocked.value && purpose == AuthPurpose.VAULT) {
            return AuthResult.Success
        }

        // DOCUMENT_EXPORT always requires fresh PIN/biometric — never skip when vault is unlocked.
        val biometricEnabled = securityPreferences.preferences.first().biometricEnabled

        if (pin != null) {
            return when (val result = pinManager.verifyPin(pin)) {
                is PinVerificationResult.Success -> {
                    vaultSessionManager.unlock()
                    AuthResult.Success
                }
                is PinVerificationResult.Locked ->
                    AuthResult.Error("Too many attempts. Try again in ${result.remainingSeconds}s.")
                PinVerificationResult.RequireReauthentication ->
                    AuthResult.Error("Too many failed attempts. Re-authentication required.")
                is PinVerificationResult.Failure ->
                    AuthResult.Error("Incorrect PIN. ${result.remainingAttempts} attempts remaining.")
                PinVerificationResult.NotConfigured ->
                    AuthResult.Error("PIN not configured.")
            }
        }

        if (biometricEnabled && biometricAuthManager.isBiometricAvailable()) {
            val bioResult = biometricAuthManager.authenticate(activity, purpose)
            if (bioResult is AuthResult.Success) {
                vaultSessionManager.unlock()
            }
            return bioResult
        }

        return AuthResult.Error("Authentication required.")
    }

    fun lockVault() {
        vaultSessionManager.lock()
    }
}
