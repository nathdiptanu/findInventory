package com.docufind.app.ocr

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Best-effort OCR field suggestions. Always require user review before save.
 */
object OcrFieldSuggestions {

    data class Suggestion(
        val issueDateMillis: Long? = null,
        val expiryDateMillis: Long? = null,
        val fieldValues: Map<String, String> = emptyMap(),
        val confidenceNote: String = "DocuFind detected these details. Please verify them before saving."
    )

    private val datePatterns = listOf(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MM/yy", Locale.ENGLISH)
    )

    private val dateRegex = Regex(
        """(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4}|\d{2}/\d{2})"""
    )
    private val ifscRegex = Regex("""\b[A-Z]{4}0[A-Z0-9]{6}\b""")
    private val vehicleRegex = Regex("""\b[A-Z]{2}\s?\d{1,2}\s?[A-Z]{1,3}\s?\d{1,4}\b""")
    private val lastFourRegex = Regex("""(?:xxxx|XXXX|\*{4}|ending\s+in)\s*(\d{4})\b""", RegexOption.IGNORE_CASE)

    fun extract(text: String, categoryId: String): Suggestion {
        val normalized = text.replace('\u00A0', ' ')
        val dates = dateRegex.findAll(normalized)
            .mapNotNull { parseDate(it.value) }
            .distinct()
            .sorted()
            .toList()

        val issue = dates.firstOrNull()
        val expiry = dates.drop(1).lastOrNull() ?: dates.lastOrNull()?.takeIf { dates.size == 1 && looksLikeExpiryContext(normalized) }

        val fields = mutableMapOf<String, String>()
        when (categoryId) {
            "finance" -> {
                ifscRegex.find(normalized.uppercase())?.value?.let { fields["ifsc"] = it }
            }
            "vehicle" -> {
                vehicleRegex.find(normalized.uppercase())?.value?.replace(" ", "")?.let {
                    fields["vehicle_number"] = it
                }
            }
            "cards" -> {
                lastFourRegex.find(normalized)?.groupValues?.getOrNull(1)?.let {
                    fields["last_four"] = it
                }
            }
            "id_cards" -> {
                Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""").find(normalized)?.value?.let {
                    fields["id_number"] = it.replace(" ", "")
                }
            }
            "insurance" -> {
                Regex("""(?:policy|pol)[^\d]{0,12}([A-Z0-9/-]{6,})""", RegexOption.IGNORE_CASE)
                    .find(normalized)?.groupValues?.getOrNull(1)?.let {
                        fields["policy_number"] = it
                    }
            }
        }

        return Suggestion(
            issueDateMillis = issue,
            expiryDateMillis = expiry?.takeIf { issue == null || it != issue },
            fieldValues = fields
        )
    }

    private fun looksLikeExpiryContext(text: String): Boolean {
        val lower = text.lowercase(Locale.ENGLISH)
        return listOf("expir", "valid till", "valid until", "due", "renew").any { lower.contains(it) }
    }

    private fun parseDate(raw: String): Long? {
        val cleaned = raw.trim()
        for (formatter in datePatterns) {
            runCatching {
                val parsed = when {
                    cleaned.matches(Regex("""\d{2}/\d{2}""")) -> {
                        val parts = cleaned.split("/")
                        LocalDate.of(2000 + parts[1].toInt(), parts[0].toInt(), 1)
                    }
                    else -> LocalDate.parse(cleaned, formatter)
                }
                return parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }
}
