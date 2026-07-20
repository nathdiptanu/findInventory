package com.docufind.app.scan

import java.net.URI

/**
 * Classifies web URLs without Android [android.net.Uri] so unit tests and
 * device code share the same scheme rules.
 */
object SafeUrlValidator {

    fun isSafeWebUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        val scheme = extractScheme(trimmed) ?: return false
        return scheme == "http" || scheme == "https"
    }

    fun classifyUrl(raw: String): QrContent.Url {
        val trimmed = raw.trim()
        return QrContent.Url(url = trimmed, isSafeScheme = isSafeWebUrl(trimmed))
    }

    fun extractScheme(raw: String): String? {
        val trimmed = raw.trim()
        val schemeEnd = trimmed.indexOf(':')
        if (schemeEnd <= 0) return null
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        if (!scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) {
            return null
        }
        return scheme
    }

    fun queryParameter(raw: String, name: String): String? {
        return runCatching {
            val uri = URI(raw.trim())
            val query = uri.rawQuery ?: return null
            query.split('&').asSequence()
                .map { it.split('=', limit = 2) }
                .firstOrNull { it.firstOrNull() == name }
                ?.getOrNull(1)
                ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
        }.getOrNull()
    }
}
