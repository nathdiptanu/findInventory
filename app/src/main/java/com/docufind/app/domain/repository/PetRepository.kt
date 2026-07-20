package com.docufind.app.domain.repository

import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.data.local.db.entity.PetRecord
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun observeAll(): Flow<List<Pet>>
    fun observeCount(): Flow<Int>
    fun observeById(id: String): Flow<Pet?>
    fun observeRecords(petId: String): Flow<List<PetRecord>>
    fun observeRecordById(recordId: String): Flow<PetRecord?>
    suspend fun savePet(pet: Pet, photoUri: String? = null, removePhoto: Boolean = false): Result<Unit>
    suspend fun deletePet(id: String)
    suspend fun saveRecord(record: PetRecord, attachmentUri: String? = null): Result<Unit>
    suspend fun deleteRecord(recordId: String)
}
