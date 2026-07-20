package com.docufind.app.export.pdf

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.AuthPurpose
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.file.SecureDelete
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authGate: AuthGate,
    private val recordCollector: PdfExportRecordCollector,
    private val pdfBuilder: WatermarkedPdfBuilder,
    private val encryptedPdfWriter: EncryptedPdfWriter
) {
    private val exportDir: File
        get() = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }

    suspend fun export(
        activity: FragmentActivity,
        request: PdfExportRequest,
        pin: CharArray? = null
    ): PdfExportResult = withContext(Dispatchers.IO) {
        ensurePdfBoxInitialized()
        when (val passwordCheck = PdfExportPasswordValidator.validate(request.exportPassword)) {
            is PdfExportPasswordValidator.ValidationResult.Invalid ->
                return@withContext PdfExportResult.Error(passwordCheck.message)
            PdfExportPasswordValidator.ValidationResult.Valid -> Unit
        }

        when (val auth = authGate.authenticateForPurpose(activity, AuthPurpose.DOCUMENT_EXPORT, pin)) {
            is AuthResult.Success -> Unit
            is AuthResult.Cancelled -> return@withContext PdfExportResult.AuthRequired("Authentication cancelled.")
            is AuthResult.Error -> return@withContext PdfExportResult.AuthRequired(auth.message)
            AuthResult.Failed -> return@withContext PdfExportResult.AuthRequired("Authentication failed.")
        }

        var tempDecrypted = emptyList<File>()
        var plainPdf: File? = null
        var encryptedPdf: File? = null
        try {
            val collected = recordCollector.collect(request).getOrElse { error ->
                return@withContext PdfExportResult.Error(error.message ?: "Could not collect export data.")
            }
            tempDecrypted = collected.tempFiles
            assertNoSecretsInContent(collected.content)

            plainPdf = File(exportDir, "plain_${System.currentTimeMillis()}.pdf")
            pdfBuilder.build(collected.content, plainPdf!!).getOrElse { error ->
                return@withContext PdfExportResult.Error(error.message ?: "Could not build PDF.")
            }

            val displayName = PdfExportFilenameSanitizer.uniqueName(collected.content.documentTitle)
            encryptedPdf = File(exportDir, displayName)
            encryptedPdfWriter.encrypt(plainPdf!!, encryptedPdf!!, request.exportPassword).getOrElse { error ->
                return@withContext PdfExportResult.Error(error.message ?: "Could not encrypt PDF.")
            }

            SecureDelete.wipeFile(plainPdf!!)
            plainPdf = null

            PdfExportResult.Ready(encryptedPdf!!, displayName)
        } catch (error: Exception) {
            encryptedPdf?.let { SecureDelete.wipeFile(it) }
            PdfExportResult.Error(error.message ?: "Export failed.")
        } finally {
            SecureMemory.wipe(request.exportPassword)
            plainPdf?.let { SecureDelete.wipeFile(it) }
            tempDecrypted.forEach { SecureDelete.wipeFile(it) }
        }
    }

    fun wipeExportCache() {
        SecureDelete.wipeDirectory(exportDir)
        exportDir.mkdirs()
    }

    private fun ensurePdfBoxInitialized() {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized = true
        }
    }

    private fun assertNoSecretsInContent(content: PdfExportContent) {
        val combined = buildString {
            append(content.documentTitle)
            content.sections.forEach { section ->
                append(section.heading)
                append(section.subtitle)
                append(section.notes)
                section.fields.forEach { (label, value) ->
                    append(label)
                    append(value)
                }
                section.attachments.forEach { attachment ->
                    append(attachment.fileName)
                    append(attachment.ocrText)
                }
            }
        }
        check(!PdfExportContentFilter.containsForbiddenSecrets(combined)) {
            "Export content contains forbidden secret markers."
        }
    }

    companion object {
        const val EXPORT_DIR = "export"
        private var pdfBoxInitialized = false
    }
}
