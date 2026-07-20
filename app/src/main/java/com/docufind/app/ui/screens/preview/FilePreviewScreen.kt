package com.docufind.app.ui.screens.preview

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.docufind.app.ui.components.ExportConfirmationDialog
import com.docufind.app.ui.components.ExportPasswordDialog
import com.docufind.app.ui.preview.PdfPreviewRenderer
import com.docufind.app.ui.screens.add.AttachmentHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    onBack: () -> Unit,
    viewModel: FilePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var unlockPin by remember { mutableStateOf("") }

    ForceSecureScreenEffect()

    LaunchedEffect(Unit) {
        viewModel.loadFile(activity)
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
                viewModel.confirmExport(
                    activity = activity,
                    onDownloaded = { path ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                activity.getString(R.string.preview_downloaded, path)
                            )
                        }
                    }
                )
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
                TextButton(onClick = {
                    when {
                        uiState.pendingExportPassword != null -> {
                            viewModel.retryPdfExportAfterUnlock(
                                activity = activity,
                                pin = unlockPin.takeIf { it.isNotBlank() }?.toCharArray(),
                                onShare = { file -> viewModel.shareExportedPdf(activity, file) }
                            )
                        }
                        uiState.pendingExportOp != null -> {
                            viewModel.performExportWithPin(
                                activity = activity,
                                pin = unlockPin.toCharArray(),
                                onDownloaded = { path ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            activity.getString(R.string.preview_downloaded, path)
                                        )
                                    }
                                }
                            )
                        }
                        else -> viewModel.loadFile(activity, unlockPin.toCharArray())
                    }
                    unlockPin = ""
                }) {
                    Text(stringResource(R.string.unlock))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissUnlockDialog()
                    onBack()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showExportConfirmDialog) {
        ExportConfirmationDialog(
            onConfirm = viewModel::confirmExportPdf,
            onDismiss = viewModel::dismissExportConfirmDialog,
            showOcrOption = uiState.previewMode == PreviewMode.Image
        )
    }

    if (uiState.showExportPasswordDialog) {
        ExportPasswordDialog(
            onConfirm = { password ->
                viewModel.performPdfExport(
                    activity = activity,
                    exportPassword = password,
                    onShare = { file -> viewModel.shareExportedPdf(activity, file) }
                )
            },
            onDismiss = viewModel::dismissExportPasswordDialog
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.delete_file_title)) },
            text = { Text(stringResource(R.string.delete_file_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFile(onBack) }) {
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.fileName.ifBlank { stringResource(R.string.preview) },
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
                    if (uiState.fileName.isNotBlank()) {
                        TextButton(onClick = viewModel::requestExportPdf) {
                            Text(stringResource(R.string.export_pdf))
                        }
                    }
                    if (uiState.decryptedFile != null) {
                        TextButton(onClick = viewModel::open) {
                            Text(stringResource(R.string.open))
                        }
                        IconButton(onClick = { viewModel.share(activity) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                    }
                    if (uiState.fileName.isNotBlank()) {
                        IconButton(onClick = viewModel::showDeleteDialog) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.previewMode) {
            PreviewMode.Loading, PreviewMode.AuthRequired -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.loading))
                }
            }
            PreviewMode.Pdf -> {
                val file = uiState.decryptedFile
                val pages = remember(file) {
                    file?.let { PdfPreviewRenderer.renderPages(it) }.orEmpty()
                }
                if (pages.isEmpty()) {
                    PreviewUnavailableContent(
                        modifier = Modifier.padding(padding),
                        uiState = uiState,
                        message = stringResource(R.string.preview_unavailable_desc),
                        onOpen = viewModel::open,
                        onShare = { viewModel.share(activity) },
                        onDownload = {
                            viewModel.download { path ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        activity.getString(R.string.preview_downloaded, path)
                                    )
                                }
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PreviewMetadataCard(uiState = uiState)
                        }
                        itemsIndexed(pages) { _, bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
            PreviewMode.Image -> {
                val file = uiState.decryptedFile
                val bitmap = remember(file) {
                    file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                }
                if (bitmap == null) {
                    PreviewUnavailableContent(
                        modifier = Modifier.padding(padding),
                        uiState = uiState,
                        message = stringResource(R.string.preview_unavailable_desc),
                        onOpen = viewModel::open,
                        onShare = { viewModel.share(activity) },
                        onDownload = {
                            viewModel.download { path ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        activity.getString(R.string.preview_downloaded, path)
                                    )
                                }
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            PreviewMetadataCard(uiState = uiState)
                        }
                        item {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
            PreviewMode.Unavailable -> {
                PreviewUnavailableContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    message = stringResource(R.string.preview_unavailable_desc),
                    onOpen = viewModel::open,
                    onShare = {
                        if (uiState.decryptedFile != null) {
                            viewModel.share(activity)
                        } else {
                            viewModel.loadFile(activity, unlockPin.takeIf { it.isNotBlank() }?.toCharArray())
                        }
                    },
                    onDownload = {
                        viewModel.download { path ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    activity.getString(R.string.preview_downloaded, path)
                                )
                            }
                        }
                    }
                )
            }
            PreviewMode.Missing -> {
                PreviewUnavailableContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    message = stringResource(R.string.file_unavailable_desc),
                    onOpen = {},
                    onShare = {},
                    onDownload = {}
                )
            }
        }
    }
}

@Composable
private fun PreviewUnavailableContent(
    modifier: Modifier = Modifier,
    uiState: FilePreviewUiState,
    message: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.preview_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        PreviewMetadataCard(uiState = uiState)
        if (uiState.decryptedFile != null) {
            DocuFindPrimaryButton(
                text = stringResource(R.string.open),
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            DocuFindPrimaryButton(
                text = stringResource(R.string.share),
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            DocuFindPrimaryButton(
                text = stringResource(R.string.download),
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun PreviewMetadataCard(uiState: FilePreviewUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        PreviewMetaLine(stringResource(R.string.preview_file_name), uiState.fileName.ifBlank { "-" })
        PreviewMetaLine(stringResource(R.string.preview_file_size), AttachmentHelper.formatFileSize(uiState.fileSize))
        PreviewMetaLine(stringResource(R.string.preview_file_type), uiState.mimeType.ifBlank { "-" })
        PreviewMetaLine(
            stringResource(R.string.preview_file_created),
            if (uiState.createdAt > 0L) formatPreviewDate(uiState.createdAt) else "-"
        )
        PreviewMetaLine(stringResource(R.string.preview_file_category), uiState.categoryLabel.ifBlank { "-" })
    }
}

@Composable
private fun PreviewMetaLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

private fun formatPreviewDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
