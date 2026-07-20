package com.docufind.app.domain.model.backup

enum class BackupStatus {
    NEVER,
    SUCCESS,
    FAILED
}

data class StorageInfo(
    val totalBytes: Long,
    val recordCount: Int,
    val fileCount: Int,
    val lastBackupAt: Long?,
    val backupStatus: BackupStatus
)
