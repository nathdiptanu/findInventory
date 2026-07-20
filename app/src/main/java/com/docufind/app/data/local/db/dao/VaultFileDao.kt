package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.docufind.app.data.local.db.entity.VaultFile
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(file: VaultFile)

    @Query("SELECT * FROM vault_files WHERE recordId = :recordId ORDER BY createdAt ASC")
    fun observeByRecordId(recordId: String): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getById(id: String): VaultFile?

    @Query("SELECT COUNT(*) FROM vault_files WHERE recordId = :recordId")
    fun observeCountByRecordId(recordId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM vault_files")
    fun observeTotalFileSize(): Flow<Long>

    @Query("SELECT COUNT(*) FROM vault_files")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM vault_files")
    fun observeTotalCount(): Flow<Int>

    @Query("DELETE FROM vault_files WHERE id = :fileId")
    suspend fun deleteById(fileId: String)

    @Query("DELETE FROM vault_files WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String)

    @Query("SELECT * FROM vault_files WHERE recordId = :recordId")
    suspend fun getAllByRecordId(recordId: String): List<VaultFile>

    @Query("SELECT * FROM vault_files ORDER BY createdAt ASC")
    fun observeAllForInsights(): Flow<List<VaultFile>>
}
