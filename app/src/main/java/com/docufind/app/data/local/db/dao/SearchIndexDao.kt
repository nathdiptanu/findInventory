package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.docufind.app.data.local.db.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SearchIndexEntity)

    @Query("DELETE FROM search_index WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String)

    @Query("SELECT * FROM search_index ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SearchIndexEntity>>

    @Query(
        """
        SELECT * FROM search_index
        WHERE (:query = '' OR searchText LIKE '%' || :query || '%')
        AND (:category = '' OR category = :category)
        AND (:familyMemberId = '' OR familyMemberId = :familyMemberId)
        AND (:petId = '' OR petId = :petId)
        AND (:favoriteOnly = 0 OR isFavorite = 1)
        AND (:mimeType = '' OR primaryMimeType = :mimeType)
        AND (:dueSoonOnly = 0 OR (expiryDate IS NOT NULL AND expiryDate > :now AND expiryDate <= :dueSoonCutoff))
        AND (:expiredOnly = 0 OR (expiryDate IS NOT NULL AND expiryDate < :now))
        ORDER BY updatedAt DESC
        """
    )
    fun searchFiltered(
        query: String,
        category: String,
        familyMemberId: String,
        petId: String,
        favoriteOnly: Int,
        mimeType: String,
        dueSoonOnly: Int,
        expiredOnly: Int,
        now: Long,
        dueSoonCutoff: Long
    ): Flow<List<SearchIndexEntity>>
}
