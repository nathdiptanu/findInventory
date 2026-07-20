package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docufind.app.data.local.db.entity.FamilyMember
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: FamilyMember)

    @Update
    suspend fun update(member: FamilyMember)

    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun observeAll(): Flow<List<FamilyMember>>

    @Query("SELECT COUNT(*) FROM family_members")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM family_members WHERE id = :id")
    fun observeById(id: String): Flow<FamilyMember?>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: String): FamilyMember?

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteById(id: String)
}
