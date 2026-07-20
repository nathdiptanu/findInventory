package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backup_metadata",
    indices = [Index(value = ["createdAt"])]
)
data class BackupMetadata(
    @PrimaryKey val id: String,
    val fileName: String,
    val localPath: String,
    val fileSize: Long,
    val recordCount: Int,
    val checksumSha256: String? = null,
    val appVersion: String? = null,
    val createdAt: Long
)
