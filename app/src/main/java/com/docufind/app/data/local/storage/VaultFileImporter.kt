package com.docufind.app.data.local.storage

import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.entity.VaultFile
import com.docufind.app.di.IoDispatcher
import com.docufind.app.security.file.CorruptedFileException
import com.docufind.app.security.file.EncryptedFileManager
import com.docufind.app.security.file.EncryptedFileMetadata
import com.docufind.app.security.file.FileTooLargeException
import com.docufind.app.security.file.SecureDelete
import com.docufind.app.security.file.UnsupportedFileTypeException
import com.docufind.app.security.logging.SecureLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class VaultImportResult {
    data class Success(val vaultFile: VaultFile) : VaultImportResult()
    data class Error(val message: String, val cause: Throwable? = null) : VaultImportResult()
}

@Singleton
class VaultFileImporter @Inject constructor(
    private val encryptedFileManager: EncryptedFileManager,
    private val vaultFileDao: VaultFileDao,
    private val storagePaths: VaultStoragePaths,
    private val thumbnailGenerator: VaultThumbnailGenerator,
    private val secureLogger: SecureLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun importFile(
        recordId: String,
        fileName: String,
        mimeType: String,
        sourceStream: InputStream,
        declaredSize: Long = -1L
    ): VaultImportResult = withContext(ioDispatcher) {
        VaultFileValidator.validateMimeType(mimeType)
        if (declaredSize >= 0) VaultFileValidator.validateDeclaredSize(declaredSize)

        val fileId = UUID.randomUUID().toString()
        val staged = encryptedFileManager.stageImport(
            sourceStream = sourceStream,
            workDir = storagePaths.tempImportDir(),
            declaredSize = declaredSize
        )
        var encrypted: EncryptedFileMetadata? = null
        var thumbnailRelative: String? = null

        try {
            thumbnailRelative = thumbnailGenerator.generateThumbnail(
                sourceFile = staged.file,
                mimeType = mimeType,
                fileId = fileId
            )

            encrypted = encryptedFileManager.encryptStagedFile(
                staging = staged,
                vaultDir = storagePaths.vaultDir(),
                mimeType = mimeType,
                fileId = fileId
            )

            val relativeEncPath = storagePaths.relativePath(
                storagePaths.vaultDir(),
                File(encrypted.encryptedPath)
            )

            val vaultFile = VaultFile(
                id = encrypted.id,
                recordId = recordId,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = encrypted.originalSize,
                localPath = relativeEncPath,
                thumbnailPath = thumbnailRelative,
                createdAt = System.currentTimeMillis()
            )
            vaultFileDao.insert(vaultFile)
            VaultImportResult.Success(vaultFile)
        } catch (e: FileTooLargeException) {
            cleanupFailedImport(encrypted, thumbnailRelative)
            VaultImportResult.Error(FILE_TOO_LARGE_MESSAGE, e)
        } catch (e: UnsupportedFileTypeException) {
            cleanupFailedImport(encrypted, thumbnailRelative)
            VaultImportResult.Error("Unsupported file type.", e)
        } catch (e: CorruptedFileException) {
            cleanupFailedImport(encrypted, thumbnailRelative)
            VaultImportResult.Error("File could not be imported.", e)
        } catch (e: Exception) {
            cleanupFailedImport(encrypted, thumbnailRelative)
            secureLogger.warn("Import failed: ${e.message}")
            VaultImportResult.Error("Import failed. Please try again.", e)
        } finally {
            SecureDelete.wipeFile(staged.file)
        }
    }

    private fun cleanupFailedImport(
        encrypted: EncryptedFileMetadata?,
        thumbnailRelative: String?
    ) {
        encrypted?.let {
            encryptedFileManager.deleteEncryptedFiles(it, storagePaths.vaultDir())
        }
        thumbnailGenerator.deleteThumbnail(thumbnailRelative)
    }

    companion object {
        const val FILE_TOO_LARGE_MESSAGE =
            "File is larger than 10 MB. Please choose a smaller file."
    }
}
