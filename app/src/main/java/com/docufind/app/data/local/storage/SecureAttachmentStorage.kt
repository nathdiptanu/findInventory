package com.docufind.app.data.local.storage

import android.content.Context
import android.net.Uri
import com.docufind.app.di.IoDispatcher
import com.docufind.app.security.file.EncryptedFileManager
import com.docufind.app.security.file.EncryptedFileMetadata
import com.docufind.app.security.file.SecureDelete
import com.docufind.app.security.file.SupportedMimeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class SecureImportResult {
    data class Success(
        val fileId: String,
        val mimeType: String,
        val displayName: String
    ) : SecureImportResult()

    data class Error(val message: String) : SecureImportResult()
}

@Singleton
class SecureAttachmentStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedFileManager: EncryptedFileManager,
    private val storagePaths: VaultStoragePaths,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun importFromUri(uri: Uri, displayName: String): SecureImportResult = withContext(ioDispatcher) {
        try {
            val mimeType = context.contentResolver.getType(uri)
                ?.takeIf { SupportedMimeType.fromMime(it) != null }
                ?: return@withContext SecureImportResult.Error("Unsupported file type")
            val fileId = UUID.randomUUID().toString()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val metadata = encryptedFileManager.encryptAndStore(
                    sourceStream = stream,
                    vaultDir = storagePaths.vaultDir(),
                    mimeType = mimeType
                )
                SecureImportResult.Success(
                    fileId = fileIdFromMetadata(metadata) ?: fileId,
                    mimeType = mimeType,
                    displayName = displayName
                )
            } ?: SecureImportResult.Error("Could not read file")
        } catch (e: Exception) {
            SecureImportResult.Error(e.message ?: "Import failed")
        }
    }

    suspend fun decryptToCache(fileId: String, mimeType: String): Result<File> = withContext(ioDispatcher) {
        runCatching {
            val vaultDir = storagePaths.vaultDir()
            val encFile = File(vaultDir, "$fileId.enc")
            val metadata = EncryptedFileMetadata(
                id = fileId,
                originalMimeType = mimeType,
                originalSize = 0,
                checksumSha256 = "",
                encryptedPath = encFile.absolutePath
            )
            val ext = when (mimeType) {
                SupportedMimeType.PDF.mime -> "pdf"
                SupportedMimeType.PNG.mime -> "png"
                else -> "jpg"
            }
            val outputDir = File(context.cacheDir, "export").apply { mkdirs() }
            val output = File(outputDir, "$fileId.$ext")
            encryptedFileManager.decryptToFile(metadata, vaultDir, output)
            output
        }
    }

    suspend fun delete(fileId: String) = withContext(ioDispatcher) {
        val vaultDir = storagePaths.vaultDir()
        val encFile = File(vaultDir, "$fileId.enc")
        val metadata = EncryptedFileMetadata(
            id = fileId,
            originalMimeType = "",
            originalSize = 0,
            checksumSha256 = "",
            encryptedPath = encFile.absolutePath
        )
        encryptedFileManager.deleteEncryptedFiles(metadata, vaultDir)
    }

    private fun fileIdFromMetadata(metadata: EncryptedFileMetadata): String? {
        return File(metadata.encryptedPath).nameWithoutExtension.takeIf { it.isNotBlank() }
    }
}
