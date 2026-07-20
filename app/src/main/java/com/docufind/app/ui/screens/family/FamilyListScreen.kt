package com.docufind.app.ui.screens.family

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.family.BloodGroup
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.module.ModuleFilterChips
import com.docufind.app.ui.components.profile.ProfileEmptyState
import com.docufind.app.ui.components.profile.ProfileListCard
import com.docufind.app.security.protection.ForceSecureScreenEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyListScreen(
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit,
    viewModel: FamilyListViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.family_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddForm) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DocuFindSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchChange,
                    placeholder = stringResource(R.string.family_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            item {
                ModuleFilterChips(
                    chips = uiState.filterChips,
                    selected = uiState.selectedFilter,
                    onSelected = viewModel::onFilterSelected
                )
            }
            if (uiState.members.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.family_empty_message))
                }
            } else if (uiState.filteredMembers.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.list_no_matches))
                }
            } else {
                items(uiState.filteredMembers, key = { it.id }) { member ->
                    val relation = FamilyRelation.fromStored(member.relationship)
                    val blood = BloodGroup.fromStored(member.bloodGroup)
                    ProfileListCard(
                        title = member.name,
                        subtitle = buildString {
                            append(relation.displayName)
                            if (blood != BloodGroup.UNKNOWN) append(" · ${blood.displayName}")
                        },
                        icon = Icons.Default.Person,
                        onClick = { onMemberClick(member.id) }
                    )
                }
            }
        }
    }

        if (uiState.showForm) {
            FamilyFormDialog(
                member = uiState.editingMember,
                existingAvatarPreviewPath = uiState.avatarPreviewPath,
                onDismiss = viewModel::dismissForm,
                onSave = { name, relation, dob, blood, phone, email, notes, photoUri, removePhoto ->
                    viewModel.saveMember(name, relation, dob, blood, phone, email, notes, photoUri, removePhoto) {}
                }
            )
        }
    }
}
