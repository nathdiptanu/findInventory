package com.docufind.app.ui.screens.support

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.support.SupportModuleOptions
import com.docufind.app.ui.components.form.DocuFindFormScaffold
import com.docufind.app.ui.components.form.DocuFindFormSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBugScreen(
    onBack: () -> Unit,
    viewModel: ReportBugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var moduleExpanded by remember { mutableStateOf(false) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val name = queryDisplayName(context, uri) ?: "screenshot.jpg"
        viewModel.onAttachmentSelected(uri, name, mimeType)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    uiState.copyReportText?.let { text ->
        CopyReportDialog(
            reportText = text,
            onDismiss = viewModel::dismissCopyDialog,
            onCopy = {
                viewModel.copyReportToClipboard()
                viewModel.dismissCopyDialog()
            }
        )
    }

    DocuFindFormScaffold(
        title = stringResource(R.string.settings_report_bug),
        onBack = onBack,
        onSave = viewModel::submit,
        saveEnabled = !uiState.isSubmitting,
        saveLabel = stringResource(R.string.bug_submit),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
        }

        DocuFindFormSection {
            OutlinedTextField(
                value = uiState.issueTitle,
                onValueChange = viewModel::onIssueTitleChange,
                label = { Text(stringResource(R.string.bug_issue_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.bug_description)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExposedDropdownMenuBox(
                expanded = moduleExpanded,
                onExpandedChange = { moduleExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.moduleAffected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.bug_module_affected)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = moduleExpanded,
                    onDismissRequest = { moduleExpanded = false }
                ) {
                    SupportModuleOptions.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onModuleChange(option)
                                moduleExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.stepsToReproduce,
                onValueChange = viewModel::onStepsChange,
                label = { Text(stringResource(R.string.bug_steps)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { attachmentLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.bug_add_screenshot))
                }
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bug_screenshot_optional),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    uiState.attachmentName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.attachmentUri != null) {
                    IconButton(onClick = viewModel::removeAttachment) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove))
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) return cursor.getString(index)
        }
    return null
}
