package com.docufind.app.security.session

import com.docufind.app.security.settings.AutoLockTimeout
import com.docufind.app.security.settings.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeoutManager @Inject constructor(
    private val vaultSessionManager: VaultSessionManager,
    private val securityPreferences: SecurityPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onUserActivity() {
        vaultSessionManager.touchActivity()
    }

    fun checkTimeoutAndLock() {
        scope.launch {
            if (!vaultSessionManager.isUnlocked.value) return@launch
            val prefs = securityPreferences.preferences.first()
            val timeoutMs = prefs.autoLockTimeout.millis
            if (timeoutMs <= 0) return@launch // "Always" locked handled separately
            val elapsed = System.currentTimeMillis() - vaultSessionManager.lastActivityTime()
            if (elapsed >= timeoutMs) {
                vaultSessionManager.lock()
            }
        }
    }
}
