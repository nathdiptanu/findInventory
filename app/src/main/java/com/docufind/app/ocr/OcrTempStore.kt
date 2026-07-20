package com.docufind.app.ocr

import android.content.Context
import android.net.Uri
import com.docufind.app.security.file.SecureDelete
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrTempStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    private val ocrDir: File
        get() = File(context.cacheDir, OCR_DIR_NAME).apply { mkdirs() }

    fun createTempFile(extension: String): File {
        val safeExt = extension.trimStart('.').ifBlank { "tmp" }
        return File(ocrDir, "ocr_${UUID.randomUUID()}.$safeExt")
    }

    fun copyUriToTemp(uri: Uri, extension: String): File {
        val tempFile = createTempFile(extension)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: run {
            SecureDelete.wipeFile(tempFile)
            throw OcrTempCopyException("Could not read selected file")
        }
        return tempFile
    }

    fun wipeTemp(file: File?) {
        file?.let { SecureDelete.wipeFile(it) }
    }

    fun wipeAll() {
        SecureDelete.wipeDirectory(ocrDir)
        ocrDir.mkdirs()
        secureLogger.info("OCR temp cache wiped")
    }

    companion object {
        const val OCR_DIR_NAME = "ocr"
    }
}

class OcrTempCopyException(message: String) : Exception(message)
