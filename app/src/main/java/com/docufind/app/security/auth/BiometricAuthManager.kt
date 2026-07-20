package com.docufind.app.security.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    fun isBiometricAvailable(): Boolean {
        return BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        purpose: AuthPurpose
    ): AuthResult = try {
        authenticateInternal(activity, purpose, allowDeviceCredential = false)
    } catch (e: Exception) {
        secureLogger.warn("Biometric prompt failed")
        AuthResult.Error(e.message ?: "Biometric authentication unavailable.")
    }

    /** Enrollment during first-run setup — biometric only, no device-credential mix. */
    suspend fun authenticateForEnrollment(activity: FragmentActivity): AuthResult = try {
        authenticateInternal(activity, AuthPurpose.VAULT, allowDeviceCredential = false)
    } catch (e: Exception) {
        secureLogger.warn("Biometric enrollment prompt failed")
        AuthResult.Error(e.message ?: "Biometric setup unavailable.")
    }

    private suspend fun authenticateInternal(
        activity: FragmentActivity,
        purpose: AuthPurpose,
        allowDeviceCredential: Boolean
    ): AuthResult = suspendCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                secureLogger.info("Biometric auth succeeded for ${purpose.name}")
                continuation.resume(AuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                secureLogger.warn("Biometric auth error")
                val result = if (
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    AuthResult.Cancelled
                } else {
                    AuthResult.Error(errString.toString())
                }
                continuation.resume(result)
            }

            override fun onAuthenticationFailed() {
                secureLogger.warn("Biometric auth failed attempt")
            }
        }

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titleForPurpose(purpose))
            .setSubtitle("Authenticate to continue")

        if (allowDeviceCredential) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            builder.setNegativeButtonText("Cancel")
        }

        BiometricPrompt(activity, executor, callback).authenticate(builder.build())
    }

    private fun titleForPurpose(purpose: AuthPurpose): String = when (purpose) {
        AuthPurpose.VAULT -> "Unlock"
        AuthPurpose.DOCUMENT_VIEW -> "View Document"
        AuthPurpose.DOCUMENT_EXPORT -> "Export Document"
        AuthPurpose.BACKUP_RESTORE -> "Restore Backup"
    }

    companion object {
        private const val BIOMETRIC_STRONG = BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}
