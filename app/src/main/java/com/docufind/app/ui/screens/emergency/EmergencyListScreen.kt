package com.docufind.app.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.profile.ProfileEmptyState
import com.docufind.app.security.protection.ForceSecureScreenEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyListScreen(
    onBack: () -> Unit,
    viewModel: EmergencyListViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    uiState.pendingDeleteId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.emergency_delete_title)) },
            text = { Text(stringResource(R.string.emergency_delete_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.emergency_title),
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
                    placeholder = stringResource(R.string.emergency_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            if (uiState.contacts.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.emergency_empty_message))
                }
            } else if (uiState.filteredContacts.isEmpty()) {
                item {
                    ProfileEmptyState(message = stringResource(R.string.list_no_matches))
                }
            } else {
                items(uiState.filteredContacts, key = { it.id }) { contact ->
                    val relation = FamilyRelation.fromStored(contact.relationship)
                    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ContactEmergency,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Row(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        append(contact.name)
                                        if (contact.isPrimary) append(" ★")
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call_now))
                            }
                            IconButton(onClick = { viewModel.showEditForm(contact) }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                            IconButton(onClick = { viewModel.requestDelete(contact.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                        Text(
                            text = "${relation.displayName} · ${contact.phone}",
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }

        if (uiState.showForm) {
            EmergencyFormDialog(
                contact = uiState.editingContact,
                familyMembers = uiState.familyMemberOptions,
                onDismiss = viewModel::dismissForm,
                onSave = { name, phone, alt, email, relation, linkedId, notes, isPrimary ->
                    viewModel.saveContact(name, phone, alt, email, relation, linkedId, notes, isPrimary)
                }
            )
        }
    }
}
