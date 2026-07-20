package com.docufind.app.data.local.db.dao



import androidx.room.Dao

import androidx.room.Insert

import androidx.room.OnConflictStrategy

import androidx.room.Query

import androidx.room.Update

import com.docufind.app.data.local.db.entity.PetRecord

import kotlinx.coroutines.flow.Flow



@Dao

interface PetRecordDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)

    suspend fun insert(record: PetRecord)



    @Update

    suspend fun update(record: PetRecord)



    @Query("SELECT * FROM pet_records WHERE petId = :petId ORDER BY recordDate DESC, updatedAt DESC")

    fun observeByPetId(petId: String): Flow<List<PetRecord>>



    @Query("SELECT * FROM pet_records WHERE id = :id")

    suspend fun getById(id: String): PetRecord?



    @Query("SELECT * FROM pet_records WHERE id = :id")

    fun observeById(id: String): Flow<PetRecord?>



    @Query("DELETE FROM pet_records WHERE id = :id")

    suspend fun deleteById(id: String)

}

