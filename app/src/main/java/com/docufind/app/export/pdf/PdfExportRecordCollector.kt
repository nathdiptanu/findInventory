package com.docufind.app.export.pdf

import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.db.entity.VaultFile
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.data.local.storage.VaultFileExporter
import com.docufind.app.domain.model.VaultCategory
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.security.metadata.SensitiveMetadataCipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExportRecordCollector @Inject constructor(
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val vaultFileExporter: VaultFileExporter,
    private val sensitiveMetadataCipher: SensitiveMetadataCipher
) {
    suspend fun collect(request: PdfExportRequest): Result<CollectionResult> = runCatching {
        val tempFiles = mutableListOf<java.io.File>()
        val records = resolveRecords(request)
        if (records.isEmpty()) {
            throw IllegalArgumentException("No records found to export.")
        }
        val sections = records.map { record ->
            val files = vaultFileDao.getAllByRecordId(record.id)
            val attachments = mutableListOf<PdfExportAttachment>()
            files.forEach { vaultFile ->
                if (request is PdfExportRequest.SingleFile && vaultFile.id != request.fileId) return@forEach
                val decrypted = vaultFileExporter.decryptToCache(vaultFile.id).getOrThrow()
                tempFiles.add(decrypted)
                attachments += PdfExportAttachment(
                    fileName = vaultFile.fileName,
                    mimeType = vaultFile.mimeType,
                    localFile = decrypted,
                    ocrText = null
                )
            }
            buildSection(record, attachments, request)
        }
        val title = when (request) {
            is PdfExportRequest.SingleFile -> sections.firstOrNull()?.heading ?: "DocuFind Export"
            is PdfExportRequest.Record -> sections.firstOrNull()?.heading ?: "DocuFind Export"
            is PdfExportRequest.Category -> {
                val module = DocuFindModule.fromId(request.categoryId)
                module?.title ?: VaultCategory.fromId(request.categoryId)?.displayName ?: "Category Export"
            }
            is PdfExportRequest.SelectedRecords -> "Selected Records"
            is PdfExportRequest.FullVaultSubset -> "Vault Export"
        }
        CollectionResult(
            content = PdfExportContent(documentTitle = title, sections = sections),
            tempFiles = tempFiles
        )
    }

    private suspend fun resolveRecords(request: PdfExportRequest): List<VaultRecord> = when (request) {
        is PdfExportRequest.SingleFile -> {
            val file = vaultFileDao.getById(request.fileId)
                ?: throw IllegalArgumentException("File not found.")
            listOfNotNull(vaultRecordDao.getById(file.recordId))
        }
        is PdfExportRequest.Record -> listOfNotNull(vaultRecordDao.getById(request.recordId))
        is PdfExportRequest.SelectedRecords -> request.recordIds.mapNotNull { vaultRecordDao.getById(it) }
        is PdfExportRequest.Category -> {
            vaultRecordDao.getAllRecords().filter { it.category == request.categoryId }
        }
        is PdfExportRequest.FullVaultSubset -> request.recordIds.mapNotNull { vaultRecordDao.getById(it) }
    }

    private fun buildSection(
        record: VaultRecord,
        attachments: List<PdfExportAttachment>,
        request: PdfExportRequest
    ): PdfExportSection {
        val moduleTitle = DocuFindModule.fromId(record.category)?.title
            ?: VaultCategory.fromId(record.category)?.displayName
            ?: record.category
        val exportFields = PdfExportContentFilter.fieldsForExport(
            record.category,
            record.tags,
            sensitiveMetadataCipher
        ).map { it.label to it.value }
        val dateFields = buildList {
            PdfExportContentFilter.formatDate(record.issueDate)?.let {
                add("Issue date" to it)
            }
            PdfExportContentFilter.formatDate(record.expiryDate)?.let {
                add("Expiry date" to it)
            }
        }
        val filteredAttachments = when (request) {
            is PdfExportRequest.SingleFile -> attachments.filter { it.fileName.isNotBlank() }
            else -> attachments
        }
        return PdfExportSection(
            heading = record.title,
            subtitle = listOfNotNull(moduleTitle, record.subCategory).joinToString(" · ").ifBlank { null },
            fields = dateFields + exportFields,
            notes = record.notes?.takeIf { it.isNotBlank() },
            attachments = filteredAttachments
        )
    }

    data class CollectionResult(
        val content: PdfExportContent,
        val tempFiles: List<java.io.File>
    )
}
