package com.docufind.app.data.repository

import android.content.Context
import android.net.Uri
import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.SearchIndexDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.search.SearchIndexBuilder
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.data.local.storage.VaultFileExporter
import com.docufind.app.data.local.storage.VaultFileImporter
import com.docufind.app.data.local.storage.VaultImportResult
import com.docufind.app.domain.model.FamilyMemberOption
import com.docufind.app.domain.model.PetOption
import com.docufind.app.domain.model.SaveDocumentRequest
import com.docufind.app.domain.model.SaveDocumentResult
import com.docufind.app.domain.model.VaultCategory
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.model.module.ModuleFileItem
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import com.docufind.app.security.session.VaultSessionManager
import com.docufind.app.domain.model.module.RecordMetadata
import com.docufind.app.domain.model.module.ModuleRecordDetail
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.model.module.ModuleRecordUpdate
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.insights.ActivityInsightsTracker
import com.docufind.app.reminder.ReminderEngine
import com.docufind.app.security.file.VaultRecordSecureDelete
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRecordRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val searchIndexDao: SearchIndexDao,
    private val familyMemberDao: FamilyMemberDao,
    private val petDao: PetDao,
    private val vaultFileImporter: VaultFileImporter,
    private val vaultFileExporter: VaultFileExporter,
    private val vaultRecordSecureDelete: VaultRecordSecureDelete,
    private val reminderEngine: ReminderEngine,
    private val searchIndexBuilder: SearchIndexBuilder,
    private val sensitiveMetadataCipher: SensitiveMetadataCipher,
    private val vaultSessionManager: VaultSessionManager,
    private val activityInsightsTracker: ActivityInsightsTracker
) : VaultRecordRepository {

    override fun observeFamilyMembers(): Flow<List<FamilyMemberOption>> =
        familyMemberDao.observeAll().map { members ->
            members.map { FamilyMemberOption(it.id, it.name) }
        }

    override fun observePets(): Flow<List<PetOption>> =
        petDao.observeAll().map { pets ->
            pets.map { PetOption(it.id, it.name) }
        }

    override fun observeModuleRecords(categoryId: String): Flow<List<ModuleRecordItem>> =
        vaultRecordDao.observeModuleListRows(categoryId).map { rows ->
            rows.map { row ->
                ModuleRecordItem(
                    id = row.id,
                    title = row.title,
                    subCategory = row.subCategory,
                    updatedAt = row.updatedAt,
                    fileCount = row.fileCount,
                    isFavorite = row.isFavorite
                )
            }
        }

    override fun observeRecordDetail(recordId: String): Flow<ModuleRecordDetail?> {
        val recordFlow = vaultRecordDao.observeById(recordId)
        val filesFlow = vaultFileDao.observeByRecordId(recordId)
        return combine(recordFlow, filesFlow) { record, files ->
            record?.let { toDetail(it, files) }
        }
    }

    override suspend fun updateRecord(update: ModuleRecordUpdate): SaveDocumentResult {
        val existing = vaultRecordDao.getById(update.recordId)
            ?: return SaveDocumentResult.Error("Record not found.")
        if (existing.deletedAt != null) return SaveDocumentResult.Error("Record is in Trash.")
        if (update.title.isBlank()) return SaveDocumentResult.Error("Title is required.")

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = update.title.trim(),
            subCategory = update.subCategory?.takeIf { it.isNotBlank() },
            notes = update.notes?.takeIf { it.isNotBlank() },
            issueDate = update.issueDate,
            expiryDate = update.expiryDate,
            renewalDate = update.expiryDate,
            updatedAt = now
        )
        return try {
            vaultRecordDao.update(updated)
            upsertSearchIndex(updated)
            reminderEngine.syncVaultRecordReminders(updated, reminderEnabled = true)
            SaveDocumentResult.Success(updated.id)
        } catch (e: Exception) {
            SaveDocumentResult.Error("Could not update record.")
        }
    }

    override suspend fun setFavorite(recordId: String, favorite: Boolean) {
        val now = System.currentTimeMillis()
        vaultRecordDao.setFavorite(recordId, favorite, now)
        val record = vaultRecordDao.getById(recordId) ?: return
        if (record.deletedAt == null) {
            upsertSearchIndex(record.copy(isFavorite = favorite, updatedAt = now))
        }
    }

    override suspend fun moveToTrash(recordId: String) {
        val now = System.currentTimeMillis()
        vaultRecordDao.softDelete(recordId, deletedAt = now, updatedAt = now)
        searchIndexDao.deleteByRecordId(recordId)
        reminderEngine.disableByLinkedRecordId(recordId)
    }

    override suspend fun restoreFromTrash(recordId: String) {
        val now = System.currentTimeMillis()
        vaultRecordDao.restoreFromTrash(recordId, updatedAt = now)
        val record = vaultRecordDao.getById(recordId) ?: return
        upsertSearchIndex(record.copy(deletedAt = null, updatedAt = now))
        reminderEngine.syncVaultRecordReminders(record.copy(deletedAt = null), reminderEnabled = true)
    }

    override suspend fun permanentlyDelete(recordId: String) {
        vaultRecordSecureDelete.deleteRecord(recordId)
    }

    override fun observeTrash(): Flow<List<ModuleRecordItem>> =
        vaultRecordDao.observeTrash().map { records ->
            records.map { record ->
                ModuleRecordItem(
                    id = record.id,
                    title = record.title,
                    subCategory = record.subCategory ?: record.category,
                    updatedAt = record.deletedAt ?: record.updatedAt,
                    fileCount = 0,
                    isFavorite = record.isFavorite
                )
            }
        }

    override fun observeExpiringSoon(limit: Int): Flow<List<ModuleRecordItem>> {
        val now = System.currentTimeMillis()
        val cutoff = now + 30L * 24 * 60 * 60 * 1000
        return vaultRecordDao.observeExpiringSoon(now, cutoff, limit).map { records ->
            records.map { record ->
                ModuleRecordItem(
                    id = record.id,
                    title = record.title,
                    subCategory = record.subCategory,
                    updatedAt = record.expiryDate ?: record.updatedAt,
                    fileCount = 0,
                    isFavorite = record.isFavorite
                )
            }
        }
    }

    override fun observeFamilyMemberRecords(familyMemberId: String): Flow<List<ModuleRecordItem>> =
        vaultRecordDao.observeByFamilyMember(familyMemberId).map { records ->
            records.map { record ->
                ModuleRecordItem(
                    id = record.id,
                    title = record.title,
                    subCategory = record.subCategory ?: record.category,
                    updatedAt = record.updatedAt,
                    fileCount = 0,
                    isFavorite = record.isFavorite
                )
            }
        }

    override suspend fun findLikelyDuplicates(
        categoryId: String,
        title: String,
        categoryFieldValues: Map<String, String>,
        excludeId: String
    ): List<String> {
        val matches = linkedSetOf<String>()
        vaultRecordDao.findTitleDuplicates(categoryId, title.trim(), excludeId)
            .forEach { matches += it.title }
        val lastFour = categoryFieldValues["last_four"]?.filter { it.isDigit() }.orEmpty()
        if (lastFour.length == 4) {
            vaultRecordDao.findTagFragmentDuplicates(categoryId, "last_four=$lastFour", excludeId)
                .forEach { matches += it.title }
        }
        val idNumber = categoryFieldValues["id_number"]?.trim().orEmpty()
        if (idNumber.length >= 4) {
            vaultRecordDao.findTagFragmentDuplicates(categoryId, "id_number=", excludeId)
                .filter { it.tags.any { tag -> tag.contains(idNumber.takeLast(4)) } }
                .forEach { matches += it.title }
        }
        val account = categoryFieldValues["account_number"]?.filter { it.isDigit() }.orEmpty()
        if (account.length >= 4) {
            vaultRecordDao.findTagFragmentDuplicates(categoryId, "account_number=", excludeId)
                .forEach { matches += it.title }
        }
        return matches.toList()
    }

    override suspend fun deleteRecord(recordId: String) {
        moveToTrash(recordId)
    }

    override suspend fun deleteFile(fileId: String): Result<Unit> = runCatching {
        vaultRecordSecureDelete.deleteFile(fileId)
    }

    override suspend fun decryptFileForShare(fileId: String): Result<File> =
        vaultFileExporter.decryptToCache(fileId)

    override suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResult {
        if (request.title.isBlank()) {
            return SaveDocumentResult.Error("Document title is required.")
        }
        if (request.categoryId.isBlank()) {
            return SaveDocumentResult.Error("Category is required.")
        }

        val recordId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val metadataTags = RecordMetadata.buildMetadataTags(
            request.categoryId,
            request.categoryFieldValues,
            sensitiveMetadataCipher
        )
        val allTags = request.tags + metadataTags

        val record = VaultRecord(
            id = recordId,
            title = request.title.trim(),
            category = request.categoryId,
            subCategory = request.subCategory?.takeIf { it.isNotBlank() },
            familyMemberId = request.familyMemberId,
            petId = request.petId,
            notes = request.notes?.takeIf { it.isNotBlank() },
            issueDate = request.issueDate,
            expiryDate = request.expiryDate,
            renewalDate = request.expiryDate,
            createdAt = now,
            updatedAt = now,
            tags = allTags
        )

        return try {
            vaultRecordDao.insert(record)

            for (entry in request.attachments) {
                val uri = Uri.parse(entry.uri)
                val stream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        vaultRecordDao.deleteById(recordId)
                        return SaveDocumentResult.Error("Could not read attached file: ${entry.attachment.displayName}")
                    }
                stream.use { input ->
                    when (
                        val result = vaultFileImporter.importFile(
                            recordId = recordId,
                            fileName = entry.attachment.displayName,
                            mimeType = entry.attachment.mimeType,
                            sourceStream = input,
                            declaredSize = entry.attachment.sizeBytes
                        )
                    ) {
                        is VaultImportResult.Error -> {
                            vaultRecordDao.deleteById(recordId)
                            return SaveDocumentResult.Error(result.message)
                        }
                        is VaultImportResult.Success -> Unit
                    }
                }
            }

            upsertSearchIndex(record)

            reminderEngine.syncVaultRecordReminders(record, reminderEnabled = request.reminderEnabled)
            activityInsightsTracker.trackDocumentAdded(record.category)

            SaveDocumentResult.Success(recordId)
        } catch (e: Exception) {
            vaultRecordDao.deleteById(recordId)
            SaveDocumentResult.Error("Could not save document. Please try again.")
        }
    }

    private fun toDetail(
        record: VaultRecord,
        files: List<com.docufind.app.data.local.db.entity.VaultFile>
    ): ModuleRecordDetail {
        val module = DocuFindModule.fromId(record.category)
        val revealSensitive = vaultSessionManager.isUnlocked.value
        val metadataFromRegistry = RecordMetadata.buildFieldValues(
            record.category,
            record.tags,
            sensitiveMetadataCipher,
            revealSensitive
        )
        return ModuleRecordDetail(
            id = record.id,
            title = record.title,
            categoryId = record.category,
            moduleTitle = module?.title ?: VaultCategory.fromId(record.category)?.displayName ?: record.category,
            subCategory = record.subCategory,
            issueDate = record.issueDate,
            expiryDate = record.expiryDate,
            notes = record.notes,
            tags = RecordMetadata.userTags(record.tags),
            metadataFields = metadataFromRegistry,
            files = files.map { file ->
                ModuleFileItem(
                    id = file.id,
                    fileName = file.fileName,
                    mimeType = file.mimeType,
                    fileSize = file.fileSize,
                    thumbnailPath = file.thumbnailPath
                )
            },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            isFavorite = record.isFavorite
        )
    }

    private suspend fun upsertSearchIndex(record: VaultRecord) {
        searchIndexDao.upsert(searchIndexBuilder.buildForRecord(record))
    }
}
