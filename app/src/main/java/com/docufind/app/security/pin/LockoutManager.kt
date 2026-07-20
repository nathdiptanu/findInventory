package com.docufind.app.security.pin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progressive lockout after failed PIN attempts:
 * 5 failures → 30s lock
 * 10 failures → 5min lock
 * 15 failures → require re-authentication
 */
@Singleton
class LockoutManager @Inject constructor(
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

    fun checkLockout(): LockoutState {
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockUntil > now) {
            return LockoutState.Locked((lockUntil - now) / 1000)
        }
        val failures = prefs.getInt(KEY_FAILURE_COUNT, 0)
        if (failures >= TIER3_THRESHOLD) {
            return LockoutState.RequireReauthentication
        }
        return LockoutState.Allowed
    }

    fun recordFailure(): LockoutState {
        val failures = prefs.getInt(KEY_FAILURE_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAILURE_COUNT, failures).apply()
        secureLogger.warn("Auth failure count incremented")

        return when {
            failures >= TIER3_THRESHOLD -> LockoutState.RequireReauthentication
            failures >= TIER2_THRESHOLD -> {
                setLockUntil(System.currentTimeMillis() + LOCK_5_MIN_MS)
                LockoutState.Locked(LOCK_5_MIN_MS / 1000)
            }
            failures >= TIER1_THRESHOLD -> {
                setLockUntil(System.currentTimeMillis() + LOCK_30_SEC_MS)
                LockoutState.Locked(LOCK_30_SEC_MS / 1000)
            }
            else -> LockoutState.Allowed
        }
    }

    fun resetAttempts() {
        prefs.edit()
            .putInt(KEY_FAILURE_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    fun remainingAttemptsBeforeLockout(): Int {
        val failures = prefs.getInt(KEY_FAILURE_COUNT, 0)
        return when {
            failures >= TIER3_THRESHOLD -> 0
            failures >= TIER2_THRESHOLD -> TIER3_THRESHOLD - failures
            failures >= TIER1_THRESHOLD -> TIER2_THRESHOLD - failures
            else -> TIER1_THRESHOLD - failures
        }
    }

    private fun setLockUntil(timestamp: Long) {
        prefs.edit().putLong(KEY_LOCK_UNTIL, timestamp).apply()
    }

    companion object {
        private const val PREFS_NAME = "docufind_lockout"
        private const val KEY_FAILURE_COUNT = "failure_count"
        private const val KEY_LOCK_UNTIL = "lock_until"
        private const val TIER1_THRESHOLD = 5
        private const val TIER2_THRESHOLD = 10
        private const val TIER3_THRESHOLD = 15
        private const val LOCK_30_SEC_MS = 30_000L
        private const val LOCK_5_MIN_MS = 300_000L
    }
}

sealed class LockoutState {
    data object Allowed : LockoutState()
    data class Locked(val remainingSeconds: Long) : LockoutState()
    data object RequireReauthentication : LockoutState()
}
