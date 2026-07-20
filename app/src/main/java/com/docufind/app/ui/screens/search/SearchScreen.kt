package com.docufind.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.domain.model.search.SearchFilterChip
import com.docufind.app.domain.model.search.SearchFilters
import com.docufind.app.domain.model.search.SearchResultItem
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindSearchBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onRecordClick: (String) -> Unit,
    initialQuery: String? = null,
    onBack: (() -> Unit)? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForceSecureScreenEffect()

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.onQueryChange(initialQuery)
            viewModel.submitSearch()
        }
    }

    uiState.activeFilterDialog?.let { chip ->
        FilterSelectionDialog(
            chip = chip,
            uiState = uiState,
            categoryOptions = viewModel.categoryOptions,
            mimeOptions = viewModel.mimeOptions,
            onDismiss = viewModel::dismissFilterDialog,
            onCategory = viewModel::selectCategory,
            onFamily = viewModel::selectFamilyMember,
            onPet = viewModel::selectPet,
            onMime = viewModel::selectMimeType
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_search),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                } else {
                    {}
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DocuFindSearchBar(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.search_documents),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    onSearch = viewModel::submitSearch
                )
            }
            item {
                SearchFilterChipRow(
                    filters = uiState.filters,
                    onChipClick = viewModel::onFilterChipClick,
                    onClearFilters = viewModel::clearFilters
                )
            }
            item {
                SearchSortRow(
                    selected = uiState.sort,
                    onSelected = viewModel::setSort
                )
            }
            if (uiState.query.isBlank() && uiState.filters.activeCount == 0) {
                if (uiState.recentSearches.isNotEmpty()) {
                    item {
                        RecentSearchesSection(
                            searches = uiState.recentSearches,
                            onSearchClick = viewModel::onRecentSearchClick,
                            onClear = viewModel::clearRecentSearches
                        )
                    }
                }
                item { SearchEmptyHint() }
            } else if (uiState.results.isEmpty()) {
                item { SearchNoResults() }
            } else {
                uiState.groupedResults.forEach { group ->
                    item {
                        Text(
                            text = group.categoryLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(group.items, key = { it.recordId }) { item ->
                        SearchResultCard(item = item, onClick = { onRecordClick(item.recordId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSortRow(
    selected: SearchSort,
    onSelected: (SearchSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.search_sort_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilterChip(
            selected = selected == SearchSort.RECENT,
            onClick = { onSelected(SearchSort.RECENT) },
            label = { Text(stringResource(R.string.search_sort_recent)) }
        )
        FilterChip(
            selected = selected == SearchSort.NAME,
            onClick = { onSelected(SearchSort.NAME) },
            label = { Text(stringResource(R.string.search_sort_name)) }
        )
        FilterChip(
            selected = selected == SearchSort.EXPIRY,
            onClick = { onSelected(SearchSort.EXPIRY) },
            label = { Text(stringResource(R.string.search_sort_expiry)) }
        )
    }
}

@Composable
private fun SearchFilterChipRow(
    filters: SearchFilters,
    onChipClick: (SearchFilterChip) -> Unit,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilterChip.entries.forEach { chip ->
            val selected = when (chip) {
                SearchFilterChip.CATEGORY -> filters.categoryId != null
                SearchFilterChip.FAMILY -> filters.familyMemberId != null
                SearchFilterChip.PET -> filters.petId != null
                SearchFilterChip.DUE_SOON -> filters.dueSoonOnly
                SearchFilterChip.EXPIRED -> filters.expiredOnly
                SearchFilterChip.FILE_TYPE -> filters.mimeType != null
                SearchFilterChip.FAVORITE -> filters.favoriteOnly
            }
            FilterChip(
                selected = selected,
                onClick = { onChipClick(chip) },
                label = { Text(chip.label) }
            )
        }
        if (filters.activeCount > 0) {
            FilterChip(
                selected = false,
                onClick = onClearFilters,
                label = { Text(stringResource(R.string.search_clear_filters)) }
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_recent),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.search_clear_recent))
            }
        }
        searches.forEach { term ->
            DocuFindCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSearchClick(term) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Text(
                        text = term,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyHint() {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.search_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.search_placeholder_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchNoResults() {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.search_no_results_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.search_no_results_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchResultCard(item: SearchResultItem, onClick: () -> Unit) {
    DocuFindCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (item.isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            item.subCategory?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val metaParts = buildList {
                item.familyMemberName?.let { add(it) }
                item.petName?.let { add(it) }
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item.expiryDate?.let { expiry ->
                Text(
                    text = stringResource(R.string.search_expires, formatDate(expiry)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterSelectionDialog(
    chip: SearchFilterChip,
    uiState: SearchUiState,
    categoryOptions: List<Pair<String, String>>,
    mimeOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onCategory: (String?) -> Unit,
    onFamily: (String?) -> Unit,
    onPet: (String?) -> Unit,
    onMime: (String?) -> Unit
) {
    val title = when (chip) {
        SearchFilterChip.CATEGORY -> stringResource(R.string.search_filter_category)
        SearchFilterChip.FAMILY -> stringResource(R.string.search_filter_family)
        SearchFilterChip.PET -> stringResource(R.string.search_filter_pet)
        SearchFilterChip.FILE_TYPE -> stringResource(R.string.search_filter_file_type)
        else -> chip.label
    }
    val options: List<Pair<String?, String>> = when (chip) {
        SearchFilterChip.CATEGORY -> listOf(null to stringResource(R.string.search_filter_all)) +
            categoryOptions.map { it.first to it.second }
        SearchFilterChip.FAMILY -> listOf(null to stringResource(R.string.search_filter_all)) +
            uiState.familyOptions.map { it.id to it.name }
        SearchFilterChip.PET -> listOf(null to stringResource(R.string.search_filter_all)) +
            uiState.petOptions.map { it.id to it.name }
        SearchFilterChip.FILE_TYPE -> listOf(null to stringResource(R.string.search_filter_all)) +
            mimeOptions.map { it.first to it.second }
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    val (id, label) = options[index]
                    TextButton(
                        onClick = {
                            when (chip) {
                                SearchFilterChip.CATEGORY -> onCategory(id)
                                SearchFilterChip.FAMILY -> onFamily(id)
                                SearchFilterChip.PET -> onPet(id)
                                SearchFilterChip.FILE_TYPE -> onMime(id)
                                else -> onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatDate(epoch: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
}
