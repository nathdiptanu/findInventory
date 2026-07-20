package com.docufind.app.scan

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrContentParser @Inject constructor() {

    fun parse(raw: String): QrContent {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return QrContent.PlainText("")

        if (isVCard(trimmed)) {
            return parseVCard(trimmed)
        }

        val lower = trimmed.lowercase()
        when {
            lower.startsWith("tel:") -> {
                PhoneNumberNormalizer.normalize(trimmed)?.let { return QrContent.Phone(it) }
            }
            lower.startsWith("mailto:") -> {
                parseMailto(trimmed)?.let { return QrContent.Email(it) }
            }
            lower.startsWith("whatsapp://") -> {
                parseWhatsAppUri(trimmed)?.let { return QrContent.WhatsApp(it) }
            }
            lower.contains("://") || SafeUrlValidator.extractScheme(trimmed) != null -> {
                // Prefer WhatsApp deep links that use https://wa.me/
                if (lower.contains("wa.me/")) {
                    parseWaMeUrl(trimmed)?.let { return QrContent.WhatsApp(it) }
                }
                return SafeUrlValidator.classifyUrl(trimmed)
            }
            lower.contains("wa.me/") -> {
                parseWaMeUrl(trimmed)?.let { return QrContent.WhatsApp(it) }
            }
        }

        if (trimmed.contains('@') && !trimmed.contains(' ') && looksLikeEmail(trimmed)) {
            return QrContent.Email(trimmed)
        }

        if (PhoneNumberNormalizer.normalize(trimmed) != null &&
            trimmed.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
        ) {
            return QrContent.Phone(PhoneNumberNormalizer.normalize(trimmed)!!)
        }

        return QrContent.PlainText(trimmed)
    }

    private fun isVCard(value: String): Boolean {
        val normalized = value.replace("\r\n", "\n").trim()
        return normalized.startsWith("BEGIN:VCARD", ignoreCase = true) &&
            normalized.contains("END:VCARD", ignoreCase = true)
    }

    private fun parseVCard(raw: String): QrContent.Contact {
        val lines = raw.replace("\r\n", "\n").lines()
        var name: String? = null
        var phone: String? = null
        var email: String? = null

        for (line in lines) {
            val upper = line.uppercase()
            when {
                upper.startsWith("FN:") -> name = line.substringAfter(':').trim().ifBlank { null }
                upper.startsWith("TEL") -> {
                    val value = line.substringAfter(':').trim()
                    phone = PhoneNumberNormalizer.normalize(value) ?: value.ifBlank { null }
                }
                upper.startsWith("EMAIL") -> {
                    email = line.substringAfter(':').trim().ifBlank { null }
                }
            }
        }

        return QrContent.Contact(
            displayName = name,
            phone = phone,
            email = email,
            rawVCard = raw.trim()
        )
    }

    private fun parseMailto(value: String): String? {
        val address = value.removePrefix("mailto:").substringBefore('?').trim()
        return address.takeIf { looksLikeEmail(it) }
    }

    private fun parseWhatsAppUri(value: String): String? {
        val phone = SafeUrlValidator.queryParameter(value, "phone")
            ?: runCatching {
                URI(value).path?.trim('/')?.substringBefore('?')?.ifBlank { null }
            }.getOrNull()
        return phone?.let { PhoneNumberNormalizer.normalize(it) ?: PhoneNumberNormalizer.digitsOnly(it) }
            ?.takeIf { it.length in 7..15 }
    }

    private fun parseWaMeUrl(value: String): String? {
        val path = runCatching { URI(value).path }.getOrNull() ?: return null
        val segment = path.trim('/').substringBefore('/').substringBefore('?')
        if (segment.isBlank()) return null
        return PhoneNumberNormalizer.normalize(segment)
            ?: segment.filter { it.isDigit() }.takeIf { it.length in 7..15 }
    }

    private fun looksLikeEmail(value: String): Boolean {
        val at = value.indexOf('@')
        if (at <= 0 || at >= value.lastIndex) return false
        return value.none { it.isWhitespace() }
    }
}
