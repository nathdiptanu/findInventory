package com.docufind.app.data.repository

import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.domain.model.AppPreferences
import com.docufind.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: PreferencesDataStore
) : PreferencesRepository {

    override val preferences: Flow<AppPreferences> = dataStore.preferences

    override suspend fun completeOnboarding() {
        dataStore.setOnboardingCompleted(true)
    }

    override suspend fun saveUserProfile(name: String, mobile: String?, email: String?) {
        dataStore.saveUserProfile(name, mobile, email)
    }

    override suspend fun setPinConfigured(configured: Boolean) {
        dataStore.setPinConfigured(configured)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.setBiometricEnabled(enabled)
    }

    override suspend fun setVaultLocked(locked: Boolean) {
        dataStore.setVaultLocked(locked)
    }

    override suspend fun scheduleHowToUseAutoExpand() {
        dataStore.scheduleHowToUseAutoExpand()
    }

    override suspend fun consumeHowToUseAutoExpand() {
        dataStore.consumeHowToUseAutoExpand()
    }

    override suspend fun requestHowToUseExpand() {
        dataStore.requestHowToUseExpand()
    }

    override suspend fun consumeHowToUseExpandRequest() {
        dataStore.consumeHowToUseExpandRequest()
    }

    override suspend fun markHowToUseIntroSeen() {
        dataStore.markHowToUseIntroSeen()
    }

    override suspend fun setDefaultReminderTimeMinutes(minutes: Int) {
        dataStore.setDefaultReminderTimeMinutes(minutes)
    }
}
