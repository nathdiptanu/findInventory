package com.docufind.app.data.local.db.dao

data class ModuleRecordRow(
    val id: String,
    val title: String,
    val subCategory: String?,
    val updatedAt: Long,
    val isFavorite: Boolean,
    val fileCount: Int
)
