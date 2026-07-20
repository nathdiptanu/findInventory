package com.docufind.app.domain.repository

import com.docufind.app.data.local.db.entity.FamilyMember
import kotlinx.coroutines.flow.Flow

interface FamilyMemberRepository {
    fun observeAll(): Flow<List<FamilyMember>>
    fun observeCount(): Flow<Int>
    fun observeById(id: String): Flow<FamilyMember?>
    suspend fun save(member: FamilyMember, photoUri: String? = null, removePhoto: Boolean = false): Result<Unit>
    suspend fun delete(id: String)
}
