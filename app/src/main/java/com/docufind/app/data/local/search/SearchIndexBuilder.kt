package com.docufind.app.data.local.search

import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.db.entity.SearchIndexEntity
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.model.module.RecordMetadata
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchIndexBuilder @Inject constructor(
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val familyMemberDao: FamilyMemberDao,
    private val petDao: PetDao
) {
    suspend fun buildForRecord(record: VaultRecord): SearchIndexEntity {
        val familyName = record.familyMemberId?.let { familyMemberDao.getById(it)?.name }
        val petName = record.petId?.let { petDao.getById(it)?.name }
        val files = vaultFileDao.getAllByRecordId(record.id)
        val fileNames = files.joinToString(" ") { it.fileName }
        val primaryMime = files.firstOrNull()?.mimeType
        val categoryLabel = DocuFindModule.fromId(record.category)?.title ?: record.category
        val issueStr = record.issueDate?.let { formatDate(it) }
        val expiryStr = record.expiryDate?.let { formatDate(it) }

        val userTags = RecordMetadata.userTags(record.tags)

        val searchText = buildSearchText(
            record.title,
            categoryLabel,
            record.category,
            record.subCategory,
            userTags.joinToString(" "),
            familyName,
            petName,
            issueStr,
            expiryStr
        )

        return SearchIndexEntity(
            recordId = record.id,
            title = record.title,
            category = record.category,
            subCategory = record.subCategory,
            tags = userTags.takeIf { it.isNotEmpty() }?.joinToString("\u001F"),
            notes = null,
            familyMemberName = familyName,
            petName = petName,
            fileNames = null,
            issueDate = record.issueDate,
            expiryDate = record.expiryDate,
            familyMemberId = record.familyMemberId,
            petId = record.petId,
            isFavorite = record.isFavorite,
            primaryMimeType = primaryMime,
            searchText = searchText,
            updatedAt = record.updatedAt
        )
    }

    suspend fun reindexAll() {
        // Used after schema migration; records loaded individually
    }

    private fun buildSearchText(vararg parts: String?): String =
        parts.filterNot { it.isNullOrBlank() }
            .joinToString(" ")
            .lowercase(Locale.getDefault())

    private fun formatDate(epochMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
            .lowercase(Locale.getDefault())
    }
}
