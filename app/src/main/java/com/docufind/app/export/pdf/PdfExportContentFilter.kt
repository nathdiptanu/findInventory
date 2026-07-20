package com.docufind.app.export.pdf

import com.docufind.app.domain.model.CategoryFieldDef
import com.docufind.app.domain.model.CategoryFieldKind
import com.docufind.app.domain.model.CategoryFieldRegistry
import com.docufind.app.domain.model.module.ModuleFieldValue
import com.docufind.app.domain.model.module.RecordMetadata
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds export-safe field values. Password fields and netbanking passwords are excluded.
 * Account numbers are masked. Vault keys and master passwords must never appear.
 */
object PdfExportContentFilter {
    private const val MASK = "••••••"
    private val forbiddenPatterns = listOf(
        "master password",
        "encryption key",
        "biometric",
        "vault key",
        "keystore"
    )

    fun fieldsForExport(
        categoryId: String,
        tags: List<String>,
        cipher: SensitiveMetadataCipher
    ): List<ModuleFieldValue> {
        val meta = RecordMetadata.parse(tags)
        return CategoryFieldRegistry.fieldsFor(categoryId).mapNotNull { def ->
            if (def.kind == CategoryFieldKind.PASSWORD) return@mapNotNull null
            if (def.key == "password" || def.key == "netbanking_password") return@mapNotNull null
            meta[def.key]?.takeIf { it.isNotBlank() }?.let { stored ->
                ModuleFieldValue(def.label, formatForExport(def, stored, cipher))
            }
        }
    }

    fun containsForbiddenSecrets(text: String): Boolean {
        val lower = text.lowercase()
        return forbiddenPatterns.any { lower.contains(it) }
    }

    fun formatDate(epoch: Long?): String? {
        epoch ?: return null
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        return Instant.ofEpochMilli(epoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    private fun formatForExport(
        def: CategoryFieldDef,
        stored: String,
        cipher: SensitiveMetadataCipher
    ): String = when (def.kind) {
        CategoryFieldKind.PASSWORD -> MASK
        CategoryFieldKind.SENSITIVE -> {
            val plaintext = if (stored.startsWith(SensitiveMetadataCipher.ENC_PREFIX)) {
                cipher.decrypt(stored.removePrefix(SensitiveMetadataCipher.ENC_PREFIX))
            } else {
                stored
            }
            if (def.key == "account_number") maskAccountNumber(plaintext) else MASK
        }
        CategoryFieldKind.DATE -> stored.toLongOrNull()?.let { formatDate(it).orEmpty() } ?: stored
        else -> stored
    }

    fun maskAccountNumber(value: String): String {
        val digits = value.filter { it.isDigit() }
        if (digits.length <= 4) return MASK
        return "•••• ${digits.takeLast(4)}"
    }
}
