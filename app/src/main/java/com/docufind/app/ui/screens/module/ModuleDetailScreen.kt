package com.docufind.app.ui.screens.module

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.domain.model.module.ModuleFileItem
import com.docufind.app.domain.model.module.ModuleRecordDetail
import com.docufind.app.ui.components.DocuFindAsyncContent
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.docufind.app.ui.components.ExportConfirmationDialog
import com.docufind.app.ui.components.ExportPasswordDialog
import com.docufind.app.ui.screens.add.AttachmentHelper
import com.docufind.app.ui.util.rememberFragmentActivity
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onPreviewFile: (String) -> Unit,
    onRequiresUnlock: (String) -> Unit = {},
    viewModel: ModuleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = rememberFragmentActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val detail = uiState.detail
    var unlockPin by remember { mutableStateOf("") }

    ForceSecureScreenEffect()

    LaunchedEffect(uiState.requiresVaultUnlock) {
        if (uiState.requiresVaultUnlock) {
            onRequiresUnlock(viewModel.recordIdForUnlock)
        }
    }

    if (uiState.requiresVaultUnlock) return

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

    if (uiState.showExportConfirmation) {
        ExportConfirmationDialog(
            onConfirm = {
                activity?.let { act ->
                    viewModel.confirmExport(
                        activity = act,
                        onShare = { file, mime -> shareFile(context, file, mime) },
                        onDownloaded = { path ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.preview_downloaded, path)
                                )
                            }
                        }
                    )
                }
            },
            onDismiss = viewModel::dismissExportConfirmation
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
                            if (uiState.pendingExportPin != null) {
                                viewModel.retryPdfExportAfterUnlock(
                                    activity = act,
                                    pin = unlockPin.takeIf { it.isNotBlank() }?.toCharArray(),
                                    onShare = { file -> shareFile(context, file, "application/pdf") }
                                )
                            } else {
                                viewModel.performPendingFileOp(
                                    activity = act,
                                    pin = unlockPin.toCharArray(),
                                    onShare = { file, mime -> shareFile(context, file, mime) },
                                    onDownloaded = { path ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.preview_downloaded, path)
                                            )
                                        }
                                    }
                                )
                            }
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
                    viewModel.performPdfExport(
                        activity = act,
                        exportPassword = password,
                        onShare = { file -> shareFile(context, file, "application/pdf") }
                    )
                }
            },
            onDismiss = viewModel::dismissExportPasswordDialog
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.delete_record_title)) },
            text = { Text(stringResource(R.string.delete_record_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord(onDeleted) }) {
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

    if (uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditDialog,
            title = { Text(stringResource(R.string.edit_record_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.editTitle,
                        onValueChange = viewModel::onEditTitleChange,
                        label = { Text(stringResource(R.string.field_document_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.editNotes,
                        onValueChange = viewModel::onEditNotesChange,
                        label = { Text(stringResource(R.string.field_notes)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveEdit { } }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.title ?: stringResource(R.string.record_detail),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        enabled = detail != null && !uiState.isDeleting
                    ) {
                        Icon(
                            imageVector = if (detail?.isFavorite == true) {
                                Icons.Default.Star
                            } else {
                                Icons.Default.StarBorder
                            },
                            contentDescription = stringResource(R.string.action_favorite)
                        )
                    }
                    IconButton(
                        onClick = viewModel::requestExportPdf,
                        enabled = detail != null && !uiState.isDeleting && !uiState.isExportingPdf
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_pdf))
                    }
                    IconButton(
                        onClick = viewModel::showEditDialog,
                        enabled = detail != null && !uiState.isDeleting
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(
                        onClick = viewModel::showDeleteDialog,
                        enabled = detail != null && !uiState.isDeleting
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        DocuFindAsyncContent(
            isLoading = uiState.isLoading,
            isEmpty = uiState.notFound,
            emptyMessage = stringResource(R.string.record_not_found),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val detail = uiState.detail ?: return@DocuFindAsyncContent
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { DetailHeader(detail) }
                if (detail.metadataFields.isNotEmpty()) {
                    item {
                        DetailFieldsCard(title = stringResource(R.string.module_details), fields = detail.metadataFields.map { it.label to it.value })
                    }
                }
                detail.issueDate?.let { date ->
                    item { DetailFieldsCard(title = stringResource(R.string.dates), fields = listOf(stringResource(R.string.field_issue_date) to formatDate(date))) }
                }
                detail.expiryDate?.let { date ->
                    item {
                        val existing = detail.issueDate?.let { listOf(stringResource(R.string.field_issue_date) to formatDate(it)) }.orEmpty()
                        DetailFieldsCard(
                            title = if (detail.issueDate == null) stringResource(R.string.dates) else "",
                            fields = existing + (stringResource(R.string.field_expiry_date) to formatDate(date))
                        )
                    }
                }
                detail.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item { DetailFieldsCard(title = stringResource(R.string.field_notes), fields = listOf("" to notes)) }
                }
                if (detail.tags.isNotEmpty()) {
                    item { DetailFieldsCard(title = stringResource(R.string.field_tags), fields = listOf("" to detail.tags.joinToString(", "))) }
                }
                if (detail.files.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.attached_files),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(detail.files, key = { it.id }) { file ->
                        FileActionCard(
                            file = file,
                            onPreview = { onPreviewFile(file.id) },
                            onShare = {
                                viewModel.tryFileOpOrUnlock(
                                    fileId = file.id,
                                    mimeType = file.mimeType,
                                    op = FileOp.SHARE
                                )
                            },
                            onDownload = {
                                viewModel.tryFileOpOrUnlock(
                                    fileId = file.id,
                                    mimeType = file.mimeType,
                                    op = FileOp.DOWNLOAD
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(detail: ModuleRecordDetail) {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = detail.moduleTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            detail.subCategory?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = stringResource(R.string.updated_on, formatDate(detail.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DetailFieldsCard(title: String, fields: List<Pair<String, String>>) {
    if (title.isNotBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fields.forEach { (label, value) ->
                if (label.isNotBlank()) {
                    Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FileActionCard(
    file: ModuleFileItem,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(text = file.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = AttachmentHelper.formatFileSize(file.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = stringResource(R.string.preview))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.download))
                }
            }
        }
    }
}

private fun formatDate(epoch: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
}

private fun shareFile(context: android.content.Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        setType(mimeType)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
