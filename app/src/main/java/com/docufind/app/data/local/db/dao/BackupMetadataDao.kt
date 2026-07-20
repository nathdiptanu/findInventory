package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.docufind.app.data.local.db.entity.BackupMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: BackupMetadata)

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupMetadata>>

    @Query("SELECT * FROM backup_metadata WHERE id = :id")
    suspend fun getById(id: String): BackupMetadata?

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<BackupMetadata?>

    @Query("DELETE FROM backup_metadata WHERE id = :id")
    suspend fun deleteById(id: String)
}
