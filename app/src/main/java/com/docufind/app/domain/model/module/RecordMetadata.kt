package com.docufind.app.domain.model.module

import com.docufind.app.domain.model.CategoryFieldDef
import com.docufind.app.domain.model.CategoryFieldKind
import com.docufind.app.domain.model.CategoryFieldRegistry
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object RecordMetadata {
    private const val PREFIX = "meta:"
    private const val MASK = "••••••"

    fun encodePlain(key: String, value: String): String = "$PREFIX$key=$value"

    fun encodeSensitive(key: String, plaintext: String, cipher: SensitiveMetadataCipher): String {
        val encrypted = cipher.encrypt(plaintext)
        return "$PREFIX$key=${SensitiveMetadataCipher.ENC_PREFIX}$encrypted"
    }

    fun parse(tags: List<String>): Map<String, String> = tags
        .filter { it.startsWith(PREFIX) }
        .mapNotNull { tag ->
            val body = tag.removePrefix(PREFIX)
            val idx = body.indexOf('=')
            if (idx <= 0) null else body.substring(0, idx) to body.substring(idx + 1)
        }
        .toMap()

    fun userTags(tags: List<String>): List<String> = tags.filter { !it.startsWith(PREFIX) }

    fun buildMetadataTags(
        categoryId: String,
        values: Map<String, String>,
        cipher: SensitiveMetadataCipher
    ): List<String> {
        val fields = CategoryFieldRegistry.fieldsFor(categoryId)
        if (fields.isEmpty()) return emptyList()
        return fields.mapNotNull { def ->
            val raw = values[def.key]?.trim().orEmpty()
            if (raw.isBlank()) return@mapNotNull null
            val stored = when (def.kind) {
                CategoryFieldKind.SENSITIVE, CategoryFieldKind.PASSWORD ->
                    encodeSensitive(def.key, raw, cipher)
                CategoryFieldKind.DATE -> encodePlain(def.key, raw)
                else -> encodePlain(def.key, raw)
            }
            stored
        }
    }

    fun buildFieldValues(
        categoryId: String,
        tags: List<String>,
        cipher: SensitiveMetadataCipher,
        revealSensitive: Boolean
    ): List<ModuleFieldValue> {
        val meta = parse(tags)
        return CategoryFieldRegistry.fieldsFor(categoryId).mapNotNull { def ->
            meta[def.key]?.takeIf { it.isNotBlank() }?.let { stored ->
                val display = formatStoredValue(def, stored, cipher, revealSensitive)
                ModuleFieldValue(def.label, display)
            }
        }
    }

    private fun formatStoredValue(
        def: CategoryFieldDef,
        stored: String,
        cipher: SensitiveMetadataCipher,
        revealSensitive: Boolean
    ): String = when (def.kind) {
        CategoryFieldKind.SENSITIVE, CategoryFieldKind.PASSWORD -> {
            if (!revealSensitive) MASK
            else if (stored.startsWith(SensitiveMetadataCipher.ENC_PREFIX)) {
                val plaintext = cipher.decrypt(stored.removePrefix(SensitiveMetadataCipher.ENC_PREFIX))
                if (def.key == "account_number") maskAccountNumber(plaintext) else plaintext
            } else {
                if (def.key == "account_number") maskAccountNumber(stored) else stored
            }
        }
        CategoryFieldKind.DATE -> stored.toLongOrNull()?.let { formatEpoch(it) } ?: stored
        else -> stored
    }

    private fun formatEpoch(epoch: Long): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        return Instant.ofEpochMilli(epoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    private fun maskAccountNumber(value: String): String {
        val digits = value.filter { it.isDigit() }
        if (digits.length <= 4) return MASK
        return "•••• ${digits.takeLast(4)}"
    }
}
