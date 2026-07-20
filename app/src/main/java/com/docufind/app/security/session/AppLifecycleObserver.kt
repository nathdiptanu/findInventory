package com.docufind.app.security.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.docufind.app.security.logging.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locks vault immediately when app goes to background.
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val vaultSessionManager: VaultSessionManager,
    private val secureLogger: SecureLogger
) : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        vaultSessionManager.lock()
        secureLogger.info("App backgrounded — vault locked")
    }
}
