package com.docufind.app.domain.repository

import com.docufind.app.domain.model.AppPreferences
import com.docufind.app.domain.model.CategorySummary
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.QuickAccessSummary
import com.docufind.app.domain.model.RecentDocument
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun completeOnboarding()
    suspend fun saveUserProfile(name: String, mobile: String?, email: String?)
    suspend fun setPinConfigured(configured: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setVaultLocked(locked: Boolean)
    suspend fun scheduleHowToUseAutoExpand()
    suspend fun consumeHowToUseAutoExpand()
    suspend fun requestHowToUseExpand()
    suspend fun consumeHowToUseExpandRequest()
    suspend fun markHowToUseIntroSeen()
    suspend fun setDefaultReminderTimeMinutes(minutes: Int)
}

interface DocumentRepository {
    fun observeCategorySummaries(): Flow<List<CategorySummary>>
    fun observeQuickAccessSummaries(): Flow<List<QuickAccessSummary>>
    fun observeRecentDocuments(limit: Int = 5): Flow<List<RecentDocument>>
}
