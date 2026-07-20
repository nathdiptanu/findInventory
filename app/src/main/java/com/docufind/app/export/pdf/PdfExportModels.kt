package com.docufind.app.export.pdf

import java.io.File

data class PdfExportContent(
    val documentTitle: String,
    val sections: List<PdfExportSection>
)

data class PdfExportSection(
    val heading: String,
    val subtitle: String? = null,
    val fields: List<Pair<String, String>> = emptyList(),
    val notes: String? = null,
    val attachments: List<PdfExportAttachment> = emptyList()
)

data class PdfExportAttachment(
    val fileName: String,
    val mimeType: String,
    val localFile: File,
    val ocrText: String? = null
)

sealed class PdfExportResult {
    data class Ready(val file: File, val displayName: String) : PdfExportResult()
    data class AuthRequired(val message: String) : PdfExportResult()
    data class Error(val message: String) : PdfExportResult()
}
