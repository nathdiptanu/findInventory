package com.docufind.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.docufind.app.domain.model.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "docufind_preferences"
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_CONFIGURED = booleanPreferencesKey("pin_configured")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val VAULT_LOCKED = booleanPreferencesKey("vault_locked")
        val HOW_TO_USE_AUTO_EXPAND_PENDING = booleanPreferencesKey("how_to_use_auto_expand_pending")
        val HOW_TO_USE_EXPAND_REQUESTED = booleanPreferencesKey("how_to_use_expand_requested")
        val HOW_TO_USE_INTRO_SEEN = booleanPreferencesKey("how_to_use_intro_seen")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val RESTORE_RESTART_REQUIRED = booleanPreferencesKey("restore_restart_required")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        val PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_MOBILE = stringPreferencesKey("user_mobile")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val DEFAULT_REMINDER_TIME_MINUTES = intPreferencesKey("default_reminder_time_minutes")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            profileCompleted = prefs[Keys.PROFILE_COMPLETED] ?: false,
            userName = prefs[Keys.USER_NAME].orEmpty(),
            userMobile = prefs[Keys.USER_MOBILE].orEmpty(),
            userEmail = prefs[Keys.USER_EMAIL].orEmpty(),
            pinConfigured = prefs[Keys.PIN_CONFIGURED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoLockMinutes = prefs[Keys.AUTO_LOCK_MINUTES] ?: 5,
            vaultLocked = prefs[Keys.VAULT_LOCKED] ?: true,
            howToUseAutoExpandPending = prefs[Keys.HOW_TO_USE_AUTO_EXPAND_PENDING] ?: false,
            howToUseExpandRequested = prefs[Keys.HOW_TO_USE_EXPAND_REQUESTED] ?: false,
            howToUseIntroSeen = prefs[Keys.HOW_TO_USE_INTRO_SEEN] ?: false,
            defaultReminderTimeMinutes = prefs[Keys.DEFAULT_REMINDER_TIME_MINUTES] ?: DEFAULT_REMINDER_TIME_MINUTES
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveUserProfile(name: String, mobile: String?, email: String?) {
        context.dataStore.edit {
            it[Keys.USER_NAME] = name
            it[Keys.PROFILE_COMPLETED] = true
            if (mobile.isNullOrBlank()) it.remove(Keys.USER_MOBILE) else it[Keys.USER_MOBILE] = mobile
            if (email.isNullOrBlank()) it.remove(Keys.USER_EMAIL) else it[Keys.USER_EMAIL] = email
        }
    }

    suspend fun setPinConfigured(configured: Boolean) {
        context.dataStore.edit { it[Keys.PIN_CONFIGURED] = configured }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setVaultLocked(locked: Boolean) {
        context.dataStore.edit { it[Keys.VAULT_LOCKED] = locked }
    }

    suspend fun scheduleHowToUseAutoExpand() {
        context.dataStore.edit { it[Keys.HOW_TO_USE_AUTO_EXPAND_PENDING] = true }
    }

    suspend fun consumeHowToUseAutoExpand() {
        context.dataStore.edit { it[Keys.HOW_TO_USE_AUTO_EXPAND_PENDING] = false }
    }

    suspend fun requestHowToUseExpand() {
        context.dataStore.edit { it[Keys.HOW_TO_USE_EXPAND_REQUESTED] = true }
    }

    suspend fun consumeHowToUseExpandRequest() {
        context.dataStore.edit { it[Keys.HOW_TO_USE_EXPAND_REQUESTED] = false }
    }

    suspend fun markHowToUseIntroSeen() {
        context.dataStore.edit { it[Keys.HOW_TO_USE_INTRO_SEEN] = true }
    }

    suspend fun setDefaultReminderTimeMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(0, MINUTES_PER_DAY - 1)
        context.dataStore.edit { it[Keys.DEFAULT_REMINDER_TIME_MINUTES] = safeMinutes }
    }

    fun observeRecentSearches(): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.RECENT_SEARCHES]
                ?.split(RECENT_SEARCH_SEPARATOR)
                ?.filter { it.isNotBlank() }
                .orEmpty()
        }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]
                ?.split(RECENT_SEARCH_SEPARATOR)
                ?.filter { it.isNotBlank() }
                .orEmpty()
            val updated = listOf(trimmed) + current.filter { it != trimmed }
            prefs[Keys.RECENT_SEARCHES] = updated.take(MAX_RECENT_SEARCHES).joinToString(RECENT_SEARCH_SEPARATOR)
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    suspend fun setRestoreRestartRequired(required: Boolean) {
        context.dataStore.edit { it[Keys.RESTORE_RESTART_REQUIRED] = required }
    }

    fun observeRestoreRestartRequired(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.RESTORE_RESTART_REQUIRED] ?: false }

    suspend fun clearRestoreRestartRequired() {
        context.dataStore.edit { it.remove(Keys.RESTORE_RESTART_REQUIRED) }
    }

    suspend fun setLastBackup(atMillis: Long, status: String) {
        context.dataStore.edit {
            it[Keys.LAST_BACKUP_AT] = atMillis
            it[Keys.LAST_BACKUP_STATUS] = status
        }
    }

    fun observeLastBackupAt(): Flow<Long?> =
        context.dataStore.data.map { it[Keys.LAST_BACKUP_AT] }

    fun observeLastBackupStatus(): Flow<String?> =
        context.dataStore.data.map { it[Keys.LAST_BACKUP_STATUS] }

    companion object {
        private const val RECENT_SEARCH_SEPARATOR = "\u001F"
        private const val MAX_RECENT_SEARCHES = 8
        private const val DEFAULT_REMINDER_TIME_MINUTES = 9 * 60
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
