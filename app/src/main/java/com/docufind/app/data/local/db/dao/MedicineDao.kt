package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docufind.app.data.local.db.entity.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(medicine: Medicine)

    @Update
    suspend fun update(medicine: Medicine)

    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun observeAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: String): Medicine?

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteById(id: String)
}
