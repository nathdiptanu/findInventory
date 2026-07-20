package com.docufind.app.security.pin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.docufind.app.security.crypto.Pbkdf2KeyDeriver
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores PIN verifier hash and salt. Never stores plaintext PIN.
 */
@Singleton
class PinStorage @Inject constructor(
    @ApplicationContext context: Context,
    private val secureLogger: SecureLogger
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePinVerifier(salt: ByteArray, hash: ByteArray) {
        prefs.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
        secureLogger.info("PIN verifier stored")
    }

    fun loadPinVerifier(): PinVerifier? {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return null
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return null
        return PinVerifier(
            salt = Base64.getDecoder().decode(saltB64),
            hash = Base64.getDecoder().decode(hashB64)
        )
    }

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun clearPin() {
        prefs.edit().clear().apply()
    }

    data class PinVerifier(val salt: ByteArray, val hash: ByteArray)

    companion object {
        private const val PREFS_NAME = "docufind_pin_secure"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
    }
}

@Singleton
class PinManager @Inject constructor(
    private val pinStorage: PinStorage,
    private val pbkdf2: Pbkdf2KeyDeriver,
    private val lockoutManager: LockoutManager,
    private val secureLogger: SecureLogger
) {
    fun setPin(pin: CharArray) {
        require(pin.size == PIN_LENGTH) { "PIN must be 6 digits" }
        val salt = pbkdf2.generateSalt()
        val hash = pbkdf2.hashForVerification(pin, salt)
        try {
            pinStorage.savePinVerifier(salt, hash)
            lockoutManager.resetAttempts()
        } finally {
            SecureMemory.wipe(salt)
            SecureMemory.wipe(hash)
            SecureMemory.wipe(pin)
        }
    }

    fun verifyPin(pin: CharArray): PinVerificationResult {
        val lockout = lockoutManager.checkLockout()
        if (lockout is LockoutState.Locked) {
            return PinVerificationResult.Locked(lockout.remainingSeconds)
        }
        if (lockout is LockoutState.RequireReauthentication) {
            return PinVerificationResult.RequireReauthentication
        }

        val verifier = pinStorage.loadPinVerifier()
            ?: return PinVerificationResult.NotConfigured

        val valid = pbkdf2.verify(pin, verifier.salt, verifier.hash)
        SecureMemory.wipe(pin)

        return if (valid) {
            lockoutManager.resetAttempts()
            PinVerificationResult.Success
        } else {
            val state = lockoutManager.recordFailure()
            secureLogger.warn("PIN verification failed")
            when (state) {
                is LockoutState.Locked -> PinVerificationResult.Locked(state.remainingSeconds)
                is LockoutState.RequireReauthentication -> PinVerificationResult.RequireReauthentication
                LockoutState.Allowed -> PinVerificationResult.Failure(
                    remainingAttempts = lockoutManager.remainingAttemptsBeforeLockout()
                )
            }
        }
    }

    fun hasPinConfigured(): Boolean = pinStorage.hasPin()

    fun clearPinForReset() {
        pinStorage.clearPin()
        lockoutManager.resetAttempts()
    }

    companion object {
        const val PIN_LENGTH = 6
    }
}

sealed class PinVerificationResult {
    data object Success : PinVerificationResult()
    data object NotConfigured : PinVerificationResult()
    data class Failure(val remainingAttempts: Int) : PinVerificationResult()
    data class Locked(val remainingSeconds: Long) : PinVerificationResult()
    data object RequireReauthentication : PinVerificationResult()
}
