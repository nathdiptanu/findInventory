package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_files",
    foreignKeys = [
        ForeignKey(
            entity = VaultRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"])]
)
data class VaultFile(
    @PrimaryKey val id: String,
    val recordId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val localPath: String,
    val thumbnailPath: String? = null,
    val createdAt: Long
)
