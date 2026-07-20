package com.docufind.app.domain.model

data class CategorySummary(
    val category: DocumentCategory,
    val itemCount: Int
)

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val profileCompleted: Boolean = false,
    val userName: String = "",
    val userMobile: String = "",
    val userEmail: String = "",
    val pinConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val vaultLocked: Boolean = true,
    val howToUseAutoExpandPending: Boolean = false,
    val howToUseExpandRequested: Boolean = false,
    val howToUseIntroSeen: Boolean = false,
    val defaultReminderTimeMinutes: Int = 9 * 60
)
