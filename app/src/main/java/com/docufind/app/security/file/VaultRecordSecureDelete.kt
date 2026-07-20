package com.docufind.app.security.file

import com.docufind.app.data.local.db.dao.SearchIndexDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.storage.VaultStoragePaths
import com.docufind.app.data.local.storage.VaultThumbnailGenerator
import com.docufind.app.reminder.ReminderEngine
import com.docufind.app.security.logging.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Securely deletes all traces of a vault record and its files.
 */
@Singleton
class VaultRecordSecureDelete @Inject constructor(
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val searchIndexDao: SearchIndexDao,
    private val encryptedFileManager: EncryptedFileManager,
    private val storagePaths: VaultStoragePaths,
    private val thumbnailGenerator: VaultThumbnailGenerator,
    private val secureFileCache: SecureFileCache,
    private val secureLogger: SecureLogger,
    private val reminderEngine: ReminderEngine
) {
    suspend fun deleteRecord(recordId: String) {
        val vaultDir = storagePaths.vaultDir()
        val files = vaultFileDao.getAllByRecordId(recordId)

        files.forEach { file -> wipeVaultFile(file) }

        vaultFileDao.deleteByRecordId(recordId)
        searchIndexDao.deleteByRecordId(recordId)
        reminderEngine.disableByLinkedRecordId(recordId)
        vaultRecordDao.deleteById(recordId)

        secureLogger.info("Vault record securely deleted")
    }

    suspend fun deleteFile(fileId: String) {
        val file = vaultFileDao.getById(fileId) ?: return
        wipeVaultFile(file)
        vaultFileDao.deleteById(fileId)
        secureLogger.info("Vault file securely deleted")
    }

    private fun wipeVaultFile(file: com.docufind.app.data.local.db.entity.VaultFile) {
        val vaultDir = storagePaths.vaultDir()
        val encFile = storagePaths.resolveRelative(vaultDir, file.localPath)
        val metadata = EncryptedFileMetadata(
            id = file.id,
            originalMimeType = file.mimeType,
            originalSize = file.fileSize,
            checksumSha256 = "",
            encryptedPath = encFile.absolutePath
        )
        encryptedFileManager.deleteEncryptedFiles(metadata, vaultDir)
        thumbnailGenerator.deleteThumbnail(file.thumbnailPath)
        secureFileCache.wipeFile(metadata)
    }
}
