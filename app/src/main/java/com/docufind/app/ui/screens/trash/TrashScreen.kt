package com.docufind.app.ui.screens.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.R
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.components.DocuFindCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: VaultRecordRepository
) : ViewModel() {
    val items: StateFlow<List<ModuleRecordItem>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: String) = viewModelScope.launch { repository.restoreFromTrash(id) }

    fun permanentlyDelete(id: String) = viewModelScope.launch { repository.permanentlyDelete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var pendingPermanentId by remember { mutableStateOf<String?>(null) }

    if (pendingPermanentId != null) {
        AlertDialog(
            onDismissRequest = { pendingPermanentId = null },
            title = { Text(stringResource(R.string.trash_delete_forever)) },
            text = { Text(stringResource(R.string.trash_delete_forever_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPermanentId?.let(viewModel::permanentlyDelete)
                        pendingPermanentId = null
                    }
                ) {
                    Text(stringResource(R.string.trash_delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.trash_empty),
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            item.subCategory?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.restore(item.id) }) {
                                    Text(stringResource(R.string.trash_restore))
                                }
                                TextButton(onClick = { pendingPermanentId = item.id }) {
                                    Text(stringResource(R.string.trash_delete_forever))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
