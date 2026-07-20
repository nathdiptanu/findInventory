package com.docufind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_index",
    indices = [
        Index(value = ["title"]),
        Index(value = ["category"]),
        Index(value = ["familyMemberId"]),
        Index(value = ["petId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["expiryDate"])
    ]
)
data class SearchIndexEntity(
    @PrimaryKey val recordId: String,
    val title: String,
    val category: String,
    val subCategory: String? = null,
    val tags: String? = null,
    val notes: String? = null,
    val familyMemberName: String? = null,
    val petName: String? = null,
    val fileNames: String? = null,
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val familyMemberId: String? = null,
    val petId: String? = null,
    val isFavorite: Boolean = false,
    val primaryMimeType: String? = null,
    val searchText: String,
    val updatedAt: Long
)
