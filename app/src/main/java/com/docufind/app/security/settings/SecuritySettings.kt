package com.docufind.app.security.settings

enum class AutoLockTimeout(val millis: Long, val label: String) {
    SEC_30(30_000L, "30 seconds"),
    MIN_1(60_000L, "1 minute"),
    MIN_2(120_000L, "2 minutes"),
    MIN_5(300_000L, "5 minutes"),
    ALWAYS(0L, "Always locked");

    companion object {
        fun fromMillis(value: Long): AutoLockTimeout =
            entries.find { it.millis == value } ?: MIN_5
    }
}

data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    val allowScreenshots: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.MIN_5
)
