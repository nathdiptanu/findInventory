package com.docufind.app.domain.repository

import com.docufind.app.data.local.db.entity.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface EmergencyContactRepository {
    fun observeAll(): Flow<List<EmergencyContact>>
    fun observeCount(): Flow<Int>
    fun observeById(id: String): Flow<EmergencyContact?>
    suspend fun save(contact: EmergencyContact): Result<Unit>
    suspend fun delete(id: String)
}
