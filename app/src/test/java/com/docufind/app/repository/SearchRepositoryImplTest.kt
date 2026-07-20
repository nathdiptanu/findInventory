package com.docufind.app.repository

import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.SearchIndexDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.data.local.search.SearchIndexBuilder
import com.docufind.app.data.repository.SearchRepositoryImpl
import com.docufind.app.domain.model.search.SearchFilters
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchRepositoryImplTest {

    private val searchIndexDao = mockk<SearchIndexDao>()
    private val vaultRecordDao = mockk<VaultRecordDao>(relaxed = true)
    private val searchIndexBuilder = mockk<SearchIndexBuilder>(relaxed = true)
    private val familyMemberDao = mockk<FamilyMemberDao>(relaxed = true)
    private val petDao = mockk<PetDao>(relaxed = true)
    private val preferencesDataStore = mockk<PreferencesDataStore>(relaxed = true)
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setup() {
        every { familyMemberDao.observeAll() } returns flowOf(emptyList())
        every { petDao.observeAll() } returns flowOf(emptyList())
        every { preferencesDataStore.observeRecentSearches() } returns flowOf(emptyList())
        repository = SearchRepositoryImpl(
            searchIndexDao,
            vaultRecordDao,
            searchIndexBuilder,
            familyMemberDao,
            petDao,
            preferencesDataStore
        )
    }

    @Test
    fun search_emptyDatabase_returnsEmptyListWithoutCrash() = runTest {
        every {
            searchIndexDao.searchFiltered(
                query = "",
                category = "",
                familyMemberId = "",
                petId = "",
                favoriteOnly = 0,
                mimeType = "",
                dueSoonOnly = 0,
                expiredOnly = 0,
                now = any(),
                dueSoonCutoff = any()
            )
        } returns flowOf(emptyList())

        val results = repository.search("", SearchFilters()).first()

        assertThat(results).isEmpty()
    }

    @Test
    fun search_withQuery_delegatesToDao() = runTest {
        every {
            searchIndexDao.searchFiltered(
                query = "passport",
                category = "",
                familyMemberId = "",
                petId = "",
                favoriteOnly = 0,
                mimeType = "",
                dueSoonOnly = 0,
                expiredOnly = 0,
                now = any(),
                dueSoonCutoff = any()
            )
        } returns flowOf(emptyList())

        repository.search("  Passport  ", SearchFilters()).first()

        // Normalized lowercase query passed to DAO
        io.mockk.verify {
            searchIndexDao.searchFiltered(
                query = "passport",
                category = any(),
                familyMemberId = any(),
                petId = any(),
                favoriteOnly = any(),
                mimeType = any(),
                dueSoonOnly = any(),
                expiredOnly = any(),
                now = any(),
                dueSoonCutoff = any()
            )
        }
    }
}
