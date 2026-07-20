package com.docufind.app.data.local.db.dao



import androidx.room.Dao

import androidx.room.Insert

import androidx.room.OnConflictStrategy

import androidx.room.Query

import androidx.room.Update

import com.docufind.app.data.local.db.entity.EmergencyContact

import kotlinx.coroutines.flow.Flow



@Dao

interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)

    suspend fun insert(contact: EmergencyContact)



    @Update

    suspend fun update(contact: EmergencyContact)



    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")

    fun observeAll(): Flow<List<EmergencyContact>>



    @Query("SELECT COUNT(*) FROM emergency_contacts")

    fun observeCount(): Flow<Int>



    @Query("SELECT * FROM emergency_contacts WHERE id = :id")

    suspend fun getById(id: String): EmergencyContact?



    @Query("SELECT * FROM emergency_contacts WHERE id = :id")

    fun observeById(id: String): Flow<EmergencyContact?>



    @Query("UPDATE emergency_contacts SET isPrimary = 0 WHERE id != :exceptId")

    suspend fun clearPrimaryExcept(exceptId: String)



    @Query("UPDATE emergency_contacts SET isPrimary = 0")

    suspend fun clearAllPrimary()



    @Query("DELETE FROM emergency_contacts WHERE id = :id")

    suspend fun deleteById(id: String)

}

