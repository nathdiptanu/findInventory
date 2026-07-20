package com.docufind.app.data.local.storage

import android.content.Context
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.security.file.EncryptedFileManager
import com.docufind.app.security.file.EncryptedFileMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultFileExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultFileDao: VaultFileDao,
    private val storagePaths: VaultStoragePaths,
    private val encryptedFileManager: EncryptedFileManager
) {
    suspend fun decryptToCache(fileId: String): Result<File> = runCatching {
        val vaultFile = vaultFileDao.getById(fileId)
            ?: throw IllegalArgumentException("File not found")
        val vaultDir = storagePaths.vaultDir()
        val encFile = storagePaths.resolveRelative(vaultDir, vaultFile.localPath)
        if (!encFile.exists()) {
            throw FileNotFoundException("Encrypted file missing")
        }
        val metadata = EncryptedFileMetadata(
            id = vaultFile.id,
            originalMimeType = vaultFile.mimeType,
            originalSize = vaultFile.fileSize,
            checksumSha256 = "",
            encryptedPath = encFile.absolutePath
        )
        val ext = when (vaultFile.mimeType) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            else -> "jpg"
        }
        val outputDir = File(context.cacheDir, "export").apply { mkdirs() }
        val output = File(outputDir, "${vaultFile.id}.$ext")
        encryptedFileManager.decryptToFile(metadata, vaultDir, output)
        output
    }
}
