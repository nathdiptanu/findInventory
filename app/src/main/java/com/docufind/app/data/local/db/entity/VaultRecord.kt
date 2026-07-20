package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_records",
    indices = [
        Index(value = ["category"]),
        Index(value = ["updatedAt"]),
        Index(value = ["familyMemberId"]),
        Index(value = ["petId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["deletedAt"])
    ]
)
data class VaultRecord(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val subCategory: String? = null,
    val familyMemberId: String? = null,
    val petId: String? = null,
    val notes: String? = null,
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val renewalDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    /** Non-sensitive category-specific field values (JSON). Sensitive values stay encrypted in tags. */
    val categoryMetadataJson: String? = null,
    /** Soft-delete timestamp; null means active. Encrypted files stay until permanent delete. */
    val deletedAt: Long? = null
)
