package com.docufind.app.data.repository

import android.content.Context
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.data.local.db.DocuFindDatabase
import com.docufind.app.data.local.db.dao.BackupMetadataDao
import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.db.entity.BackupMetadata
import com.docufind.app.data.local.storage.VaultStoragePaths
import com.docufind.app.domain.model.backup.BackupStatus
import com.docufind.app.domain.model.backup.StorageInfo
import com.docufind.app.domain.repository.BackupRepository
import com.docufind.app.security.backup.BackupBuildResult
import com.docufind.app.security.backup.BackupManager
import com.docufind.app.security.backup.BackupPreview
import com.docufind.app.security.backup.RestoreManager
import com.docufind.app.security.backup.RestoreResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val reminderDao: ReminderDao,
    private val backupMetadataDao: BackupMetadataDao,
    private val preferencesDataStore: PreferencesDataStore,
    private val vaultStoragePaths: VaultStoragePaths
) : BackupRepository {

    override fun observeStorageInfo(): Flow<StorageInfo> = combine(
        vaultRecordDao.observeTotalCount(),
        vaultFileDao.observeTotalCount(),
        vaultFileDao.observeTotalFileSize(),
        preferencesDataStore.observeLastBackupAt(),
        preferencesDataStore.observeLastBackupStatus()
    ) { recordCount, fileCount, vaultBytes, lastBackupAt, lastStatus ->
        val dbFile = context.getDatabasePath(DocuFindDatabase.DATABASE_NAME)
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val vaultDirBytes = directorySize(vaultStoragePaths.vaultDir())
        StorageInfo(
            totalBytes = vaultBytes + dbBytes + vaultDirBytes,
            recordCount = recordCount,
            fileCount = fileCount,
            lastBackupAt = lastBackupAt,
            backupStatus = lastStatus?.let { runCatching { BackupStatus.valueOf(it) }.getOrNull() }
                ?: BackupStatus.NEVER
        )
    }.flowOn(Dispatchers.IO).catch {
        emit(
            StorageInfo(
                totalBytes = 0L,
                recordCount = 0,
                fileCount = 0,
                lastBackupAt = null,
                backupStatus = BackupStatus.NEVER
            )
        )
    }

    override suspend fun createBackup(password: CharArray): Result<BackupBuildResult> {
        val recordCount = vaultRecordDao.getAllRecords().size
        val fileCount = vaultFileDao.countAll()
        val reminderCount = reminderDao.countAll()
        return backupManager.createEncryptedBackup(
            password = password,
            recordCount = recordCount,
            fileCount = fileCount,
            reminderCount = reminderCount
        )
    }

    override suspend fun validateRestore(
        backupBytes: ByteArray,
        password: CharArray
    ): RestoreResult = restoreManager.validateAndPreview(backupBytes, password)

    override suspend fun restoreBackup(
        backupBytes: ByteArray,
        password: CharArray
    ): RestoreResult = restoreManager.restoreConfirmed(backupBytes, password)

    override suspend fun recordBackupExport(
        fileName: String,
        localPath: String,
        fileSize: Long,
        preview: BackupPreview
    ) {
        backupMetadataDao.insert(
            BackupMetadata(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                localPath = localPath,
                fileSize = fileSize,
                recordCount = preview.recordCount,
                checksumSha256 = null,
                appVersion = preview.appVersion,
                createdAt = preview.createdAtMillis
            )
        )
        preferencesDataStore.setLastBackup(preview.createdAtMillis, BackupStatus.SUCCESS.name)
    }

    override suspend fun markBackupFailed() {
        preferencesDataStore.setLastBackup(
            System.currentTimeMillis(),
            BackupStatus.FAILED.name
        )
    }

    override fun observeRestoreRestartRequired(): Flow<Boolean> =
        preferencesDataStore.observeRestoreRestartRequired()

    override suspend fun clearRestoreRestartRequired() {
        preferencesDataStore.clearRestoreRestartRequired()
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return try {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }
}
