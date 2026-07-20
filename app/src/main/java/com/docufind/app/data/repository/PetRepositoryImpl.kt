package com.docufind.app.data.repository

import android.net.Uri
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.PetRecordDao
import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.data.local.db.entity.PetRecord
import com.docufind.app.data.local.storage.SecureAttachmentStorage
import com.docufind.app.data.local.storage.SecureImportResult
import com.docufind.app.domain.repository.PetRepository
import com.docufind.app.reminder.ReminderEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PetRepositoryImpl @Inject constructor(
    private val petDao: PetDao,
    private val petRecordDao: PetRecordDao,
    private val secureAttachmentStorage: SecureAttachmentStorage,
    private val reminderEngine: ReminderEngine
) : PetRepository {

    override fun observeAll(): Flow<List<Pet>> = petDao.observeAll()

    override fun observeCount(): Flow<Int> = petDao.observeCount()

    override fun observeById(id: String): Flow<Pet?> = petDao.observeById(id)

    override fun observeRecords(petId: String): Flow<List<PetRecord>> = petRecordDao.observeByPetId(petId)

    override fun observeRecordById(recordId: String): Flow<PetRecord?> = petRecordDao.observeById(recordId)

    override suspend fun savePet(pet: Pet, photoUri: String?, removePhoto: Boolean): Result<Unit> = runCatching {
        var photoPath = pet.photoPath
        if (removePhoto) {
            pet.photoPath?.let { secureAttachmentStorage.delete(it) }
            photoPath = null
        } else if (photoUri != null) {
            pet.photoPath?.let { secureAttachmentStorage.delete(it) }
            when (val result = secureAttachmentStorage.importFromUri(Uri.parse(photoUri), pet.name)) {
                is SecureImportResult.Success -> photoPath = result.fileId
                is SecureImportResult.Error -> throw IllegalStateException(result.message)
            }
        }
        val toSave = pet.copy(photoPath = photoPath)
        val existing = petDao.getById(pet.id)
        if (existing == null) {
            petDao.insert(toSave)
        } else {
            petDao.update(toSave)
        }
    }

    override suspend fun deletePet(id: String) {
        val pet = petDao.getById(id) ?: return
        pet.photoPath?.let { secureAttachmentStorage.delete(it) }
        petDao.deleteById(id)
    }

    override suspend fun saveRecord(record: PetRecord, attachmentUri: String?): Result<Unit> = runCatching {
        var attachmentPath = record.attachmentPath
        var attachmentMime = record.attachmentMimeType
        var attachmentName = record.attachmentName
        if (attachmentUri != null) {
            record.attachmentPath?.let { secureAttachmentStorage.delete(it) }
            when (val result = secureAttachmentStorage.importFromUri(Uri.parse(attachmentUri), record.title)) {
                is SecureImportResult.Success -> {
                    attachmentPath = result.fileId
                    attachmentMime = result.mimeType
                    attachmentName = result.displayName
                }
                is SecureImportResult.Error -> throw IllegalStateException(result.message)
            }
        }
        val toSave = record.copy(
            attachmentPath = attachmentPath,
            attachmentMimeType = attachmentMime,
            attachmentName = attachmentName
        )
        val existing = petRecordDao.getById(record.id)
        if (existing == null) {
            petRecordDao.insert(toSave)
        } else {
            petRecordDao.update(toSave)
        }
        syncPetRecordReminder(toSave)
    }

    override suspend fun deleteRecord(recordId: String) {
        val record = petRecordDao.getById(recordId) ?: return
        record.attachmentPath?.let { secureAttachmentStorage.delete(it) }
        reminderEngine.disableByLinkedPetRecordId(recordId)
        petRecordDao.deleteById(recordId)
    }

    private suspend fun syncPetRecordReminder(record: PetRecord) {
        val pet = petDao.getById(record.petId)
        reminderEngine.syncPetVaccination(record, pet)
    }
}
