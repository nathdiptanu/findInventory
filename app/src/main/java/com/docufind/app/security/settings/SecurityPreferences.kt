package com.docufind.app.security.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "docufind_security_preferences"
)

@Singleton
class SecurityPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val ALLOW_SCREENSHOTS = booleanPreferencesKey("allow_screenshots")
        val AUTO_LOCK_TIMEOUT_MS = longPreferencesKey("auto_lock_timeout_ms")
    }

    val preferences: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        SecuritySettings(
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            allowScreenshots = prefs[Keys.ALLOW_SCREENSHOTS] ?: false,
            autoLockTimeout = AutoLockTimeout.fromMillis(
                prefs[Keys.AUTO_LOCK_TIMEOUT_MS] ?: AutoLockTimeout.MIN_5.millis
            )
        )
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAllowScreenshots(allowed: Boolean) {
        context.securityDataStore.edit { it[Keys.ALLOW_SCREENSHOTS] = allowed }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        context.securityDataStore.edit { it[Keys.AUTO_LOCK_TIMEOUT_MS] = timeout.millis }
    }
}
