package com.docufind.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.domain.model.backup.BackupStatus
import com.docufind.app.security.backup.BackupPreview
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindEmptyState
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.docufind.app.util.AppRestartHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }

    ForceSecureScreenEffect()
    val scope = rememberCoroutineScope()
    var pendingCreateBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingCreateFileName by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val bytes = pendingCreateBytes ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("Could not write backup")
                viewModel.onBackupSaved(pendingCreateFileName, uri.toString(), bytes)
            }.onFailure {
                viewModel.markBackupFailedPublic()
                snackbarHostState.showSnackbar(context.getString(R.string.backup_save_failed))
            }
            pendingCreateBytes = null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch
            viewModel.onBackupFileSelected(bytes)
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (uiState.showRestoreConfirm) {
        uiState.restorePreview?.let { preview ->
            RestorePreviewDialog(
                preview = preview,
                onDismiss = viewModel::dismissRestoreConfirm,
                onConfirm = { viewModel.confirmRestore(activity) }
            )
        }
    }

    if (uiState.showRestoreComplete) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreComplete,
            title = { Text(stringResource(R.string.restore_complete_title)) },
            text = { Text(stringResource(R.string.restore_complete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissRestoreComplete()
                    AppRestartHelper.restartApp(context)
                }) {
                    Text(stringResource(R.string.restore_restart_now))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.docufind.app.ui.components.DocuFindLogo(size = 28.dp)
                        Text(stringResource(R.string.settings_backup), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.backupStatus == BackupStatus.NEVER) {
                DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                    DocuFindEmptyState(message = stringResource(R.string.backup_empty))
                }
            }

            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.backup_google_drive_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_google_drive_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.googleEmail.isNullOrBlank()) {
                        DocuFindPrimaryButton(
                            text = stringResource(R.string.backup_google_sign_in),
                            onClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.backup_google_signed_as, uiState.googleEmail.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.hasDriveBackup) {
                            Text(
                                text = stringResource(R.string.backup_drive_present),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (uiState.isDriveBusy) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            DocuFindPrimaryButton(
                                text = stringResource(R.string.backup_drive_upload),
                                onClick = {
                                    viewModel.uploadEncryptedBackupToDrive { intent ->
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DocuFindPrimaryButton(
                                text = stringResource(R.string.backup_drive_download),
                                onClick = {
                                    viewModel.downloadEncryptedBackupFromDrive()
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = {
                                viewModel.switchGoogleAccount { intent ->
                                    googleSignInLauncher.launch(intent)
                                }
                            }) {
                                Text(stringResource(R.string.backup_google_switch_account))
                            }
                            TextButton(onClick = viewModel::signOutGoogle) {
                                Text(stringResource(R.string.backup_google_sign_out))
                            }
                        }
                    }
                }
            }

            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.backup_create_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_password_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                        value = uiState.backupPassword,
                        onValueChange = viewModel::onBackupPasswordChange,
                        label = { Text(stringResource(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = { Text(stringResource(R.string.backup_confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        DocuFindPrimaryButton(
                            text = stringResource(R.string.backup_create_action),
                            onClick = {
                                viewModel.prepareCreateBackup { bytes, fileName ->
                                    pendingCreateBytes = bytes
                                    pendingCreateFileName = fileName
                                    createDocumentLauncher.launch(fileName)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.backup_restore_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_restore_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.backup_choose_file),
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.restorePassword,
                        onValueChange = viewModel::onRestorePasswordChange,
                        label = { Text(stringResource(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        DocuFindPrimaryButton(
                            text = stringResource(R.string.backup_validate_action),
                            onClick = viewModel::validateRestoreFile,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestorePreviewDialog(
    preview: BackupPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.restore_preview_records, preview.recordCount))
                Text(stringResource(R.string.restore_preview_files, preview.fileCount))
                Text(stringResource(R.string.restore_preview_reminders, preview.reminderCount))
                Text(stringResource(R.string.restore_preview_date, formatDate(preview.createdAtMillis)))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatDate(epoch: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatter)
}
