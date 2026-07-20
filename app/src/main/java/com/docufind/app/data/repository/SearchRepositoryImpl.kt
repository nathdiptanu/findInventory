package com.docufind.app.data.repository

import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.SearchIndexDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.data.local.db.entity.SearchIndexEntity
import com.docufind.app.data.local.search.SearchIndexBuilder
import com.docufind.app.domain.model.FamilyMemberOption
import com.docufind.app.domain.model.PetOption
import com.docufind.app.domain.model.search.SearchFilters
import com.docufind.app.domain.model.search.SearchResultItem
import com.docufind.app.domain.model.search.categoryLabel
import com.docufind.app.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchIndexDao: SearchIndexDao,
    private val vaultRecordDao: VaultRecordDao,
    private val searchIndexBuilder: SearchIndexBuilder,
    private val familyMemberDao: FamilyMemberDao,
    private val petDao: PetDao,
    private val preferencesDataStore: PreferencesDataStore
) : SearchRepository {

    override fun search(query: String, filters: SearchFilters): Flow<List<SearchResultItem>> {
        val normalized = query.trim().lowercase()
        val now = System.currentTimeMillis()
        val dueSoonCutoff = now + DUE_SOON_WINDOW_MS
        return searchIndexDao.searchFiltered(
            query = normalized,
            category = filters.categoryId.orEmpty(),
            familyMemberId = filters.familyMemberId.orEmpty(),
            petId = filters.petId.orEmpty(),
            favoriteOnly = if (filters.favoriteOnly) 1 else 0,
            mimeType = filters.mimeType.orEmpty(),
            dueSoonOnly = if (filters.dueSoonOnly) 1 else 0,
            expiredOnly = if (filters.expiredOnly) 1 else 0,
            now = now,
            dueSoonCutoff = dueSoonCutoff
        ).map { entries -> entries.map(::toResultItem) }
    }

    override fun observeRecentSearches(): Flow<List<String>> =
        preferencesDataStore.observeRecentSearches()

    override suspend fun addRecentSearch(query: String) {
        preferencesDataStore.addRecentSearch(query)
    }

    override suspend fun clearRecentSearches() {
        preferencesDataStore.clearRecentSearches()
    }

    override fun observeFamilyOptions(): Flow<List<FamilyMemberOption>> =
        familyMemberDao.observeAll().map { members ->
            members.map { FamilyMemberOption(it.id, it.name) }
        }

    override fun observePetOptions(): Flow<List<PetOption>> =
        petDao.observeAll().map { pets ->
            pets.map { PetOption(it.id, it.name) }
        }

    override suspend fun reindexAllRecords() {
        vaultRecordDao.getAllRecords().forEach { record ->
            searchIndexDao.upsert(searchIndexBuilder.buildForRecord(record))
        }
    }

    private fun toResultItem(entity: SearchIndexEntity): SearchResultItem =
        SearchResultItem(
            recordId = entity.recordId,
            title = entity.title,
            categoryId = entity.category,
            categoryLabel = categoryLabel(entity.category),
            subCategory = entity.subCategory,
            familyMemberName = entity.familyMemberName,
            petName = entity.petName,
            expiryDate = entity.expiryDate,
            isFavorite = entity.isFavorite,
            updatedAt = entity.updatedAt
        )

    companion object {
        private const val DUE_SOON_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    }
}
