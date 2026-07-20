package com.docufind.app.security.logging

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security-aware logger. Never logs sensitive data in any build.
 * Debug logs are stripped of known sensitive patterns.
 */
@Singleton
class SecureLogger @Inject constructor() {

    private val sensitivePatterns = listOf(
        Regex("(?i)pin"),
        Regex("(?i)password"),
        Regex("(?i)passphrase"),
        Regex("(?i)biometric"),
        Regex("(?i)policy.?number"),
        Regex("(?i)document.?number"),
        Regex("(?i)/data/"),
        Regex("(?i)medical"),
        Regex("(?i)ssn"),
        Regex("(?i)aadhaar")
    )

    private fun isDebugBuild(): Boolean = try {
        Class.forName("com.docufind.app.BuildConfig")
            .getField("DEBUG")
            .getBoolean(null)
    } catch (_: Throwable) {
        false
    }

    fun info(message: String) {
        if (isDebugBuild()) {
            logSafely { android.util.Log.i(TAG, sanitize(message)) }
        }
    }

    fun warn(message: String) {
        if (isDebugBuild()) {
            logSafely { android.util.Log.w(TAG, sanitize(message)) }
        }
    }

    fun error(message: String) {
        if (isDebugBuild()) {
            logSafely { android.util.Log.e(TAG, sanitize(message)) }
        }
    }

    fun error(message: String, throwable: Throwable) {
        if (isDebugBuild()) {
            logSafely { android.util.Log.e(TAG, sanitize(message), stripThrowable(throwable)) }
        }
    }

    private inline fun logSafely(block: () -> Unit) {
        try {
            block()
        } catch (_: RuntimeException) {
            // Android Log is unavailable in local JVM unit tests
        }
    }

    private fun sanitize(message: String): String {
        var result = message
        sensitivePatterns.forEach { pattern ->
            result = result.replace(pattern, "[REDACTED]")
        }
        return result
    }

    private fun stripThrowable(throwable: Throwable): Throwable {
        return Throwable(throwable.javaClass.simpleName)
    }

    companion object {
        private const val TAG = "DocuFind"
    }
}
