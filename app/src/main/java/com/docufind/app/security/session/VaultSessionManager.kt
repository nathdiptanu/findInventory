package com.docufind.app.security.session

import com.docufind.app.security.logging.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSessionManager @Inject constructor(
    private val secureLogger: SecureLogger
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _lastActivityTime = MutableStateFlow(System.currentTimeMillis())

    fun unlock() {
        _isUnlocked.value = true
        touchActivity()
        secureLogger.info("Vault session unlocked")
    }

    fun lock() {
        _isUnlocked.value = false
        secureLogger.info("Vault session locked")
    }

    fun touchActivity() {
        _lastActivityTime.value = System.currentTimeMillis()
    }

    fun lastActivityTime(): Long = _lastActivityTime.value
}
