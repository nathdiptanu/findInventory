package com.docufind.app.domain.repository

import com.docufind.app.domain.model.backup.StorageInfo
import com.docufind.app.security.backup.BackupBuildResult
import com.docufind.app.security.backup.BackupPreview
import com.docufind.app.security.backup.RestoreResult
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun observeStorageInfo(): Flow<StorageInfo>
    suspend fun createBackup(password: CharArray): Result<BackupBuildResult>
    suspend fun validateRestore(backupBytes: ByteArray, password: CharArray): RestoreResult
    suspend fun restoreBackup(backupBytes: ByteArray, password: CharArray): RestoreResult
    suspend fun recordBackupExport(
        fileName: String,
        localPath: String,
        fileSize: Long,
        preview: BackupPreview
    )
    suspend fun markBackupFailed()
    fun observeRestoreRestartRequired(): Flow<Boolean>
    suspend fun clearRestoreRestartRequired()
}
