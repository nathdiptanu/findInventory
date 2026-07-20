package com.docufind.app.ui.screens.pets

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
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.docufind.app.domain.model.pets.PetType
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.module.ModuleFilterChips
import com.docufind.app.ui.components.profile.ProfileEmptyState
import com.docufind.app.ui.components.profile.ProfileListCard
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.theme.DocuFindPetTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetListScreen(
    onBack: () -> Unit,
    onPetClick: (String) -> Unit,
    viewModel: PetListViewModel = hiltViewModel()
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

    if (uiState.showForm) {
        PetFormDialog(
            pet = uiState.editingPet,
            onDismiss = viewModel::dismissForm,
            onSave = { name, type, breed, gender, birth, weight, color, chip, vet, vetPhone, notes, photo, removePhoto ->
                viewModel.savePet(
                    name,
                    type,
                    breed,
                    gender,
                    birth,
                    weight,
                    color,
                    chip,
                    vet,
                    vetPhone,
                    notes,
                    photo,
                    removePhoto
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.pets_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddForm,
                containerColor = DocuFindPetTeal
            ) {
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
                    placeholder = stringResource(R.string.pets_search_hint),
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
            if (uiState.pets.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.pets_empty_message))
                }
            } else if (uiState.filteredPets.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.list_no_matches))
                }
            } else {
                items(uiState.filteredPets, key = { it.id }) { pet ->
                    val type = PetType.fromStored(pet.petType)
                    ProfileListCard(
                        title = pet.name,
                        subtitle = buildString {
                            append(type.displayName)
                            pet.breed?.let { append(" · $it") }
                        },
                        icon = Icons.Default.Pets,
                        iconTint = DocuFindPetTeal,
                        onClick = { onPetClick(pet.id) }
                    )
                }
            }
        }
    }
}
