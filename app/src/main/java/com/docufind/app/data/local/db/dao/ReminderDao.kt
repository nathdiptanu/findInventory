package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docufind.app.data.local.db.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY triggerAt ASC")
    fun observeActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE sourceKey = :sourceKey")
    suspend fun getBySourceKey(sourceKey: String): Reminder?

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE'")
    suspend fun getAllActive(): List<Reminder>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'ACTIVE'")
    fun observeActiveCount(): Flow<Int>

    @Query("UPDATE reminders SET status = 'DISABLED' WHERE linkedRecordId = :recordId")
    suspend fun disableByLinkedRecordId(recordId: String)

    @Query("UPDATE reminders SET status = 'DISABLED' WHERE linkedPetRecordId = :petRecordId")
    suspend fun disableByLinkedPetRecordId(petRecordId: String)

    @Query("UPDATE reminders SET status = 'DISABLED' WHERE linkedMedicineId = :medicineId")
    suspend fun disableByLinkedMedicineId(medicineId: String)

    @Query("DELETE FROM reminders WHERE linkedPetRecordId = :petRecordId")
    suspend fun deleteByPetRecordId(petRecordId: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun countAll(): Int

    @Query("UPDATE reminders SET status = 'COMPLETED', actionedAt = :actionedAt WHERE id = :id")
    suspend fun markCompleted(id: String, actionedAt: Long = System.currentTimeMillis())
}
