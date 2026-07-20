package com.docufind.app.data.repository

import com.docufind.app.data.local.db.dao.EmergencyContactDao
import com.docufind.app.data.local.db.entity.EmergencyContact
import com.docufind.app.domain.repository.EmergencyContactRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class EmergencyContactRepositoryImpl @Inject constructor(
    private val emergencyContactDao: EmergencyContactDao
) : EmergencyContactRepository {

    override fun observeAll(): Flow<List<EmergencyContact>> = emergencyContactDao.observeAll()

    override fun observeCount(): Flow<Int> = emergencyContactDao.observeCount()

    override fun observeById(id: String): Flow<EmergencyContact?> = emergencyContactDao.observeById(id)

    override suspend fun save(contact: EmergencyContact): Result<Unit> = runCatching {
        if (contact.isPrimary) {
            emergencyContactDao.clearPrimaryExcept(contact.id)
        }
        val existing = emergencyContactDao.getById(contact.id)
        if (existing == null) {
            emergencyContactDao.insert(contact)
        } else {
            emergencyContactDao.update(contact)
        }
    }

    override suspend fun delete(id: String) {
        emergencyContactDao.deleteById(id)
    }
}
