package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docufind.app.data.local.db.entity.VaultRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: VaultRecord)

    @Update
    suspend fun update(record: VaultRecord)

    @Query("SELECT * FROM vault_records WHERE id = :id")
    suspend fun getById(id: String): VaultRecord?

    @Query("SELECT * FROM vault_records WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<VaultRecord?>

    @Query("SELECT COUNT(*) FROM vault_records WHERE category = :category AND deletedAt IS NULL")
    fun observeCountByCategory(category: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_records WHERE deletedAt IS NULL")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM vault_records WHERE category = :category AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<VaultRecord>>

    @Query("SELECT * FROM vault_records WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<VaultRecord>>

    @Query("SELECT * FROM vault_records WHERE isFavorite = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<VaultRecord>>

    @Query(
        """
        SELECT r.id AS id, r.title AS title, r.subCategory AS subCategory,
               r.updatedAt AS updatedAt, r.isFavorite AS isFavorite,
               COALESCE((SELECT COUNT(*) FROM vault_files f WHERE f.recordId = r.id), 0) AS fileCount
        FROM vault_records r
        WHERE r.category = :category AND r.deletedAt IS NULL
        ORDER BY r.updatedAt DESC
        """
    )
    fun observeModuleListRows(category: String): Flow<List<ModuleRecordRow>>

    @Query(
        """
        SELECT * FROM vault_records
        WHERE deletedAt IS NULL
          AND expiryDate IS NOT NULL
          AND expiryDate >= :now
          AND expiryDate <= :cutoff
        ORDER BY expiryDate ASC
        LIMIT :limit
        """
    )
    fun observeExpiringSoon(now: Long, cutoff: Long, limit: Int): Flow<List<VaultRecord>>

    @Query("SELECT * FROM vault_records WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<VaultRecord>>

    @Query(
        """
        SELECT * FROM vault_records
        WHERE deletedAt IS NULL
          AND familyMemberId = :familyMemberId
        ORDER BY updatedAt DESC
        """
    )
    fun observeByFamilyMember(familyMemberId: String): Flow<List<VaultRecord>>

    @Query(
        """
        SELECT * FROM vault_records
        WHERE deletedAt IS NULL
          AND category = :category
          AND lower(title) = lower(:title)
          AND id != :excludeId
        LIMIT 5
        """
    )
    suspend fun findTitleDuplicates(category: String, title: String, excludeId: String): List<VaultRecord>

    @Query(
        """
        SELECT * FROM vault_records
        WHERE deletedAt IS NULL
          AND category = :category
          AND tags LIKE '%' || :tagFragment || '%'
          AND id != :excludeId
        LIMIT 5
        """
    )
    suspend fun findTagFragmentDuplicates(
        category: String,
        tagFragment: String,
        excludeId: String
    ): List<VaultRecord>

    @Query("DELETE FROM vault_records WHERE id = :recordId")
    suspend fun deleteById(recordId: String)

    @Query("SELECT * FROM vault_records WHERE deletedAt IS NULL")
    suspend fun getAllRecords(): List<VaultRecord>

    @Query("SELECT * FROM vault_records WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeAllForInsights(): Flow<List<VaultRecord>>

    @Query("UPDATE vault_records SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :recordId")
    suspend fun setFavorite(recordId: String, favorite: Boolean, updatedAt: Long)

    @Query("UPDATE vault_records SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :recordId")
    suspend fun softDelete(recordId: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE vault_records SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :recordId")
    suspend fun restoreFromTrash(recordId: String, updatedAt: Long)
}
