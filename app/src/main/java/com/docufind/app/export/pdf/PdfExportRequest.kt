package com.docufind.app.export.pdf

/**
 * Describes what to include in a password-protected PDF export.
 * [exportPassword] is wiped by [PdfExportManager] as soon as encryption completes.
 */
sealed class PdfExportRequest {
    abstract val includeOcr: Boolean
    abstract val exportPassword: CharArray

    data class SingleFile(
        val fileId: String,
        override val includeOcr: Boolean = false,
        override val exportPassword: CharArray
    ) : PdfExportRequest()

    data class Record(
        val recordId: String,
        override val includeOcr: Boolean = false,
        override val exportPassword: CharArray
    ) : PdfExportRequest()

    data class SelectedRecords(
        val recordIds: List<String>,
        override val includeOcr: Boolean = false,
        override val exportPassword: CharArray
    ) : PdfExportRequest()

    data class Category(
        val categoryId: String,
        override val includeOcr: Boolean = false,
        override val exportPassword: CharArray
    ) : PdfExportRequest()

    data class FullVaultSubset(
        val recordIds: List<String>,
        override val includeOcr: Boolean = false,
        override val exportPassword: CharArray
    ) : PdfExportRequest()
}
