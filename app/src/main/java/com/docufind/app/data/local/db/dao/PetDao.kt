package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docufind.app.data.local.db.entity.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pet: Pet)

    @Update
    suspend fun update(pet: Pet)

    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun observeAll(): Flow<List<Pet>>

    @Query("SELECT COUNT(*) FROM pets")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pets WHERE id = :id")
    fun observeById(id: String): Flow<Pet?>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getById(id: String): Pet?

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun deleteById(id: String)
}
