package com.docufind.app.export.pdf

import java.text.Normalizer

object PdfExportFilenameSanitizer {
    private val unsafeChars = Regex("""[^\w.\- \u0900-\u097F\u0980-\u09FF\u0A00-\u0A7F\u0B00-\u0B7F\u0C00-\u0C7F\u0D00-\u0D7F\u0590-\u05FF\u0600-\u06FF\u0900-\u097F]""")

    fun sanitize(baseName: String): String {
        val normalized = Normalizer.normalize(baseName.trim(), Normalizer.Form.NFC)
        val cleaned = normalized
            .replace(unsafeChars, "_")
            .replace(Regex("_+"), "_")
            .trim(' ', '.', '_')
            .ifBlank { "DocuFind_Export" }
            .take(80)
        return "${cleaned}_DocuFind.pdf"
    }

    fun uniqueName(baseName: String): String {
        val sanitized = sanitize(baseName)
        val dot = sanitized.lastIndexOf('.')
        val stem = if (dot > 0) sanitized.substring(0, dot) else sanitized
        val ext = if (dot > 0) sanitized.substring(dot) else ".pdf"
        return "${stem}_${System.currentTimeMillis()}$ext"
    }
}
