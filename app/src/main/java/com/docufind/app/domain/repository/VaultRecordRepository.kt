package com.docufind.app.domain.repository

import com.docufind.app.domain.model.FamilyMemberOption
import com.docufind.app.domain.model.PetOption
import com.docufind.app.domain.model.SaveDocumentRequest
import com.docufind.app.domain.model.SaveDocumentResult
import com.docufind.app.domain.model.module.ModuleRecordDetail
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.model.module.ModuleRecordUpdate
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VaultRecordRepository {
    fun observeFamilyMembers(): Flow<List<FamilyMemberOption>>
    fun observePets(): Flow<List<PetOption>>
    suspend fun saveDocument(request: SaveDocumentRequest): SaveDocumentResult
    fun observeModuleRecords(categoryId: String): Flow<List<ModuleRecordItem>>
    fun observeRecordDetail(recordId: String): Flow<ModuleRecordDetail?>
    suspend fun updateRecord(update: ModuleRecordUpdate): SaveDocumentResult
    suspend fun setFavorite(recordId: String, favorite: Boolean)
    suspend fun moveToTrash(recordId: String)
    suspend fun restoreFromTrash(recordId: String)
    suspend fun permanentlyDelete(recordId: String)
    fun observeTrash(): Flow<List<ModuleRecordItem>>
    fun observeExpiringSoon(limit: Int = 5): Flow<List<ModuleRecordItem>>
    fun observeFamilyMemberRecords(familyMemberId: String): Flow<List<ModuleRecordItem>>
    suspend fun findLikelyDuplicates(
        categoryId: String,
        title: String,
        categoryFieldValues: Map<String, String>,
        excludeId: String = ""
    ): List<String>
    suspend fun deleteRecord(recordId: String)
    suspend fun deleteFile(fileId: String): Result<Unit>
    suspend fun decryptFileForShare(fileId: String): Result<File>
}
