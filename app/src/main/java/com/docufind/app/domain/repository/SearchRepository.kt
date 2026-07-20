package com.docufind.app.domain.repository

import com.docufind.app.domain.model.FamilyMemberOption
import com.docufind.app.domain.model.PetOption
import com.docufind.app.domain.model.search.SearchFilters
import com.docufind.app.domain.model.search.SearchResultItem
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, filters: SearchFilters): Flow<List<SearchResultItem>>
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
    fun observeFamilyOptions(): Flow<List<FamilyMemberOption>>
    fun observePetOptions(): Flow<List<PetOption>>
    suspend fun reindexAllRecords()
}
