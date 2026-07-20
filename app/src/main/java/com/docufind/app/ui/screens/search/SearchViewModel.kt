package com.docufind.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.FamilyMemberOption
import com.docufind.app.domain.model.PetOption
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.model.search.SearchFilterChip
import com.docufind.app.domain.model.search.SearchFilters
import com.docufind.app.domain.model.search.SearchMimeFilters
import com.docufind.app.domain.model.search.SearchResultItem
import com.docufind.app.domain.repository.SearchRepository
import com.docufind.app.insights.ActivityInsightsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchSort { RECENT, NAME, EXPIRY }

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val sort: SearchSort = SearchSort.RECENT,
    val results: List<SearchResultItem> = emptyList(),
    val groupedResults: List<SearchResultGroup> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val familyOptions: List<FamilyMemberOption> = emptyList(),
    val petOptions: List<PetOption> = emptyList(),
    val activeFilterDialog: SearchFilterChip? = null,
    val isSearching: Boolean = false
)

data class SearchResultGroup(
    val categoryLabel: String,
    val items: List<SearchResultItem>
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val activityInsightsTracker: ActivityInsightsTracker
) : ViewModel() {

    private val queryInput = MutableStateFlow("")
    private val debouncedQuery = queryInput.debounce(SEARCH_DEBOUNCE_MS)
    private val filters = MutableStateFlow(SearchFilters())
    private val sort = MutableStateFlow(SearchSort.RECENT)
    private val _activeFilterDialog = MutableStateFlow<SearchFilterChip?>(null)

    private val resultsFlow = combine(debouncedQuery, filters) { q, f -> q to f }
        .flatMapLatest { (q, f) -> searchRepository.search(q, f) }

    val uiState: StateFlow<SearchUiState> = combine(
        queryInput,
        debouncedQuery,
        filters,
        sort,
        resultsFlow,
        searchRepository.observeRecentSearches(),
        searchRepository.observeFamilyOptions(),
        searchRepository.observePetOptions(),
        _activeFilterDialog
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val inputQuery = values[0] as String
        @Suppress("UNCHECKED_CAST")
        val activeQuery = values[1] as String
        @Suppress("UNCHECKED_CAST")
        val activeFilters = values[2] as SearchFilters
        @Suppress("UNCHECKED_CAST")
        val activeSort = values[3] as SearchSort
        @Suppress("UNCHECKED_CAST")
        val rawResults = values[4] as List<SearchResultItem>
        @Suppress("UNCHECKED_CAST")
        val recent = values[5] as List<String>
        @Suppress("UNCHECKED_CAST")
        val family = values[6] as List<FamilyMemberOption>
        @Suppress("UNCHECKED_CAST")
        val pets = values[7] as List<PetOption>
        val dialog = values[8] as SearchFilterChip?

        val results = when (activeSort) {
            SearchSort.RECENT -> rawResults.sortedByDescending { it.updatedAt }
            SearchSort.NAME -> rawResults.sortedBy { it.title.lowercase() }
            SearchSort.EXPIRY -> rawResults.sortedWith(
                compareBy(nullsLast()) { it.expiryDate }
            )
        }

        val grouped = results
            .groupBy { it.categoryLabel }
            .toSortedMap()
            .map { (label, items) -> SearchResultGroup(label, items) }

        SearchUiState(
            query = inputQuery,
            filters = activeFilters,
            sort = activeSort,
            results = results,
            groupedResults = grouped,
            recentSearches = recent,
            familyOptions = family,
            petOptions = pets,
            activeFilterDialog = dialog,
            isSearching = inputQuery.trim() != activeQuery.trim()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setSort(value: SearchSort) {
        sort.value = value
    }

    fun onQueryChange(value: String) {
        queryInput.value = value
    }

    fun submitSearch() {
        val trimmed = queryInput.value.trim()
        if (trimmed.isBlank()) return
        activityInsightsTracker.trackSearchUsed()
        viewModelScope.launch { searchRepository.addRecentSearch(trimmed) }
    }

    fun onRecentSearchClick(value: String) {
        queryInput.value = value
        submitSearch()
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchRepository.clearRecentSearches() }
    }

    fun onFilterChipClick(chip: SearchFilterChip) {
        when (chip) {
            SearchFilterChip.DUE_SOON -> filters.update { it.copy(dueSoonOnly = !it.dueSoonOnly) }
            SearchFilterChip.EXPIRED -> filters.update { it.copy(expiredOnly = !it.expiredOnly) }
            SearchFilterChip.FAVORITE -> filters.update { it.copy(favoriteOnly = !it.favoriteOnly) }
            else -> _activeFilterDialog.value = chip
        }
    }

    fun dismissFilterDialog() {
        _activeFilterDialog.value = null
    }

    fun selectCategory(categoryId: String?) {
        filters.update { it.copy(categoryId = categoryId) }
        dismissFilterDialog()
    }

    fun selectFamilyMember(memberId: String?) {
        filters.update { it.copy(familyMemberId = memberId) }
        dismissFilterDialog()
    }

    fun selectPet(petId: String?) {
        filters.update { it.copy(petId = petId) }
        dismissFilterDialog()
    }

    fun selectMimeType(mimeType: String?) {
        filters.update { it.copy(mimeType = mimeType) }
        dismissFilterDialog()
    }

    fun clearFilters() {
        filters.value = SearchFilters()
    }

    val categoryOptions: List<Pair<String, String>> =
        DocuFindModule.coreModules.map { it.id to it.title }

    val mimeOptions: List<Pair<String, String>> = SearchMimeFilters.options

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
