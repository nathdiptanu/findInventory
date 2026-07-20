package com.docufind.app.ui.screens.module

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.BuildConfig
import com.docufind.app.R
import com.docufind.app.ui.components.DocuFindAsyncContent
import com.docufind.app.ui.components.DocuFindEmptyState
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.ExportConfirmationDialog
import com.docufind.app.ui.components.ExportPasswordDialog
import com.docufind.app.ui.components.module.ModuleEmptyState
import com.docufind.app.ui.components.module.ModuleFilterChips
import com.docufind.app.ui.components.module.ModuleRecordCard
import com.docufind.app.ui.util.rememberFragmentActivity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleListScreen(
    onBack: () -> Unit,
    onRecordClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: ModuleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val moduleTitle = uiState.module?.title ?: stringResource(R.string.documents)
    val context = LocalContext.current
    val activity = rememberFragmentActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    var unlockPin by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    if (uiState.showExportConfirmDialog) {
        ExportConfirmationDialog(
            onConfirm = viewModel::confirmExportPdf,
            onDismiss = viewModel::dismissExportConfirmDialog,
            showOcrOption = true
        )
    }
    if (uiState.showExportPasswordDialog) {
        ExportPasswordDialog(
            onConfirm = { password ->
                activity?.let { act ->
                    viewModel.performCategoryPdfExport(
                        activity = act,
                        exportPassword = password,
                        onShare = { file -> sharePdf(context, file) }
                    )
                }
            },
            onDismiss = viewModel::dismissExportPasswordDialog
        )
    }
    if (uiState.showUnlockDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUnlockDialog,
            title = { Text(stringResource(R.string.preview_unlock_title)) },
            text = {
                Column {
                    uiState.unlockError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = unlockPin,
                        onValueChange = { unlockPin = it },
                        label = { Text(stringResource(R.string.unlock)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activity?.let { act ->
                            viewModel.retryCategoryPdfAfterUnlock(
                                activity = act,
                                pin = unlockPin.takeIf { it.isNotBlank() }?.toCharArray(),
                                onShare = { file -> sharePdf(context, file) }
                            )
                        }
                        unlockPin = ""
                    }
                ) {
                    Text(stringResource(R.string.unlock))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUnlockDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(moduleTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::requestExportCategoryPdf,
                        enabled = uiState.items.isNotEmpty() && !uiState.isExportingPdf
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.module_export_category_pdf)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))
            }
        }
    ) { padding ->
        DocuFindAsyncContent(
            isLoading = uiState.isLoading,
            errorMessage = null,
            isEmpty = false,
            onRetry = viewModel::retry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DocuFindSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchChange,
                        placeholder = stringResource(R.string.module_search_hint, moduleTitle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                item {
                    uiState.module?.let { module ->
                        ModuleFilterChips(
                            chips = module.filterChips,
                            selected = uiState.selectedFilter,
                            onSelected = viewModel::onFilterSelected
                        )
                    }
                }
                if (uiState.items.isEmpty()) {
                    item {
                        ModuleEmptyState(
                            categoryId = uiState.module?.id ?: "documents",
                            moduleTitle = moduleTitle
                        )
                    }
                } else if (uiState.filteredItems.isEmpty()) {
                    item {
                        DocuFindEmptyState(message = stringResource(R.string.list_no_matches))
                    }
                } else {
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        ModuleRecordCard(
                            item = item,
                            onClick = { onRecordClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_pdf)))
}
