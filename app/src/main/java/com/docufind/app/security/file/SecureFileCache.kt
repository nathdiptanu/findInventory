package com.docufind.app.security.file

import android.content.Context
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decrypts files to a secure cache for preview only.
 * Cache is wiped after preview closes or on session lock.
 */
@Singleton
class SecureFileCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedFileManager: EncryptedFileManager,
    private val secureLogger: SecureLogger
) {
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }

    fun decryptForPreview(metadata: EncryptedFileMetadata, vaultDir: File): File {
        wipeAll()
        val ext = SupportedMimeType.fromMime(metadata.originalMimeType)?.extension ?: "bin"
        val tempFile = File(cacheDir, "preview_${metadata.id}.$ext")
        try {
            encryptedFileManager.decryptToFile(metadata, vaultDir, tempFile)
        } catch (e: Exception) {
            SecureDelete.wipeFile(tempFile)
            throw e
        }
        secureLogger.info("Preview cache created")
        return tempFile
    }

    fun wipeAll() {
        SecureDelete.wipeDirectory(cacheDir)
        cacheDir.mkdirs()
        wipeExportCache()
        secureLogger.info("Preview cache wiped")
    }

    fun wipeExportCache() {
        val exportDir = File(context.cacheDir, EXPORT_DIR_NAME)
        SecureDelete.wipeDirectory(exportDir)
        exportDir.mkdirs()
        secureLogger.info("Export cache wiped")
    }

    fun wipeFile(metadata: EncryptedFileMetadata) {
        cacheDir.listFiles()?.filter { it.name.contains(metadata.id) }?.forEach {
            SecureDelete.wipeFile(it)
        }
    }

    companion object {
        private const val CACHE_DIR_NAME = "secure_preview"
        private const val EXPORT_DIR_NAME = "export"
    }
}
