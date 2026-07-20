package com.docufind.app.ui.screens.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.family.BloodGroup
import com.docufind.app.domain.model.family.FamilyRelation
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.util.formatDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenDocument: (String) -> Unit = {},
    viewModel: FamilyDetailViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val member = uiState.member
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.delete_member_title)) },
            text = { Text(stringResource(R.string.delete_member_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMember(onDeleted) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
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
                        member?.name ?: stringResource(R.string.family_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showEditForm) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        member?.let { m ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow(stringResource(R.string.field_relation), FamilyRelation.fromStored(m.relationship).displayName)
                            DetailRow(stringResource(R.string.field_blood_group), BloodGroup.fromStored(m.bloodGroup).displayName)
                            m.dateOfBirth?.let {
                                DetailRow(stringResource(R.string.field_dob_optional), formatDisplayDate(it))
                            }
                            m.phone?.let { DetailRow(stringResource(R.string.field_phone), it) }
                            m.email?.let { DetailRow(stringResource(R.string.field_email), it) }
                            m.notes?.let { DetailRow(stringResource(R.string.field_notes), it) }
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.family_documents),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.linkedDocuments.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.family_documents_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.linkedDocuments, key = { it.id }) { doc ->
                        DocuFindCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(doc.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(doc.title, fontWeight = FontWeight.SemiBold)
                                doc.subCategory?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        if (uiState.showForm && member != null) {
            FamilyFormDialog(
                member = member,
                existingAvatarPreviewPath = uiState.avatarPreviewPath,
                onDismiss = viewModel::dismissForm,
                onSave = { name, relation, dob, blood, phone, email, notes, photoUri, removePhoto ->
                    viewModel.saveMember(name, relation, dob, blood, phone, email, notes, photoUri, removePhoto)
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
