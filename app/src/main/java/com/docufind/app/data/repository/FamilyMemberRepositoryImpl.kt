package com.docufind.app.data.repository

import android.net.Uri
import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.entity.FamilyMember
import com.docufind.app.data.local.storage.SecureAttachmentStorage
import com.docufind.app.data.local.storage.SecureImportResult
import com.docufind.app.domain.repository.FamilyMemberRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FamilyMemberRepositoryImpl @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val secureAttachmentStorage: SecureAttachmentStorage
) : FamilyMemberRepository {

    override fun observeAll(): Flow<List<FamilyMember>> = familyMemberDao.observeAll()

    override fun observeCount(): Flow<Int> = familyMemberDao.observeCount()

    override fun observeById(id: String): Flow<FamilyMember?> = familyMemberDao.observeById(id)

    override suspend fun save(member: FamilyMember, photoUri: String?, removePhoto: Boolean): Result<Unit> = runCatching {
        var avatarPath = member.avatarPath
        if (removePhoto) {
            member.avatarPath?.let { secureAttachmentStorage.delete(it) }
            avatarPath = null
        } else if (photoUri != null) {
            member.avatarPath?.let { secureAttachmentStorage.delete(it) }
            when (val result = secureAttachmentStorage.importFromUri(Uri.parse(photoUri), member.name)) {
                is SecureImportResult.Success -> avatarPath = result.fileId
                is SecureImportResult.Error -> throw IllegalStateException(result.message)
            }
        }
        val toSave = member.copy(avatarPath = avatarPath)
        val existing = familyMemberDao.getById(member.id)
        if (existing == null) {
            familyMemberDao.insert(toSave)
        } else {
            familyMemberDao.update(toSave)
        }
    }

    override suspend fun delete(id: String) {
        val member = familyMemberDao.getById(id) ?: return
        member.avatarPath?.let { secureAttachmentStorage.delete(it) }
        familyMemberDao.deleteById(id)
    }
}
