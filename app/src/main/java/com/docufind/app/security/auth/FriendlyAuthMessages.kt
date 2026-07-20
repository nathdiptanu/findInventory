package com.docufind.app.security.auth

object FriendlyAuthMessages {
    fun forUnlock(raw: String): String = when {
        raw.contains("Incorrect PIN", ignoreCase = true) ->
            "That PIN didn't match. Please try again."
        raw.contains("attempts remaining", ignoreCase = true) ->
            "That PIN didn't match. Please try again."
        raw.contains("Too many attempts", ignoreCase = true) ||
            raw.contains("Try again in", ignoreCase = true) -> raw
        raw.contains("Re-authentication", ignoreCase = true) ->
            "Too many failed attempts. Use Forgot PIN or try again later."
        raw.contains("not configured", ignoreCase = true) ->
            "Set up a PIN in Vault first."
        raw.contains("Biometric", ignoreCase = true) ||
            raw.contains("fingerprint", ignoreCase = true) ->
            "Biometric didn't match. Use your PIN instead."
        raw.contains("Authentication required", ignoreCase = true) -> ""
        else -> raw.ifBlank { "Could not unlock. Try your PIN." }
    }
}
