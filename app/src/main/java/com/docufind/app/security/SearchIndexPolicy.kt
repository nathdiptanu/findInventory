package com.docufind.app.security

/**
 * Metadata-only search index policy.
 * Never index decrypted document body content.
 */
object SearchIndexPolicy {
    const val POLICY =
        "Search indexes may contain document title, category, and user-defined tags only. " +
            "Never store decrypted file content, OCR text, or extracted document numbers."

    /** OCR / extracted document text must never enter the search index. */
    fun allowsOcrTextInSearchIndex(): Boolean = false
}
