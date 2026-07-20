package com.docufind.app.ui.screens.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.BuildConfig
import com.docufind.app.R
import com.docufind.app.domain.model.CategoryFieldRegistry
import com.docufind.app.ui.components.AddSourceOptionsRow
import com.docufind.app.ui.components.AttachmentListSection
import com.docufind.app.ui.components.DocuFindDateField
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.docufind.app.ui.components.FilePreviewCard
import com.docufind.app.ui.components.TagItTwoWaysSection
import com.docufind.app.ui.components.form.CategoryFieldsForm
import com.docufind.app.ui.components.form.DocuFindCategoryPicker
import com.docufind.app.ui.components.form.DocuFindOptionPicker
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.reminder.ReminderDateTimeFormatter
import com.docufind.app.reminder.ReminderScheduleDefaults
import com.docufind.app.reminder.ReminderTriggerCalculator
import com.docufind.app.ui.screens.setup.SecuritySetupFlow
import android.text.format.DateFormat
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDocumentScreen(
    onDocumentSaved: (String) -> Unit = {},
    viewModel: AddDocumentViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.document_saved_message)

    fun handleAttachment(uri: Uri, fallbackName: String = "document", previewId: String = UUID.randomUUID().toString()) {
        when (val result = AttachmentHelper.resolveAttachment(context, uri, fallbackName, previewId)) {
            is AttachmentResolveResult.Success -> viewModel.onAttachmentSelected(result.info)
            is AttachmentResolveResult.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(result.messageResId))
                }
            }
        }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                handleAttachment(uri, "scan_${System.currentTimeMillis()}.jpg")
            }
        }
    }

    fun launchCamera() {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val photoFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            photoFile
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach { uri ->
            handleAttachment(uri, previewId = UUID.randomUUID().toString())
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            handleAttachment(it)
        }
    }

    fun openFilePicker() {
        fileLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
    }

    LaunchedEffect(uiState.showSuccessMessage, uiState.savedRecordId) {
        if (uiState.showSuccessMessage) {
            snackbarHostState.showSnackbar(savedMessage)
            val recordId = uiState.savedRecordId
            viewModel.consumeSuccess()
            if (!recordId.isNullOrBlank()) {
                onDocumentSaved(recordId)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.ocrStatusMessageResId) {
        uiState.ocrStatusMessageResId?.let { messageResId ->
            snackbarHostState.showSnackbar(context.getString(messageResId))
            viewModel.consumeOcrStatusMessage()
        }
    }

    if (uiState.showOcrReview) {
        OcrReviewSheet(
            extractedText = uiState.ocrReviewText,
            onAccept = viewModel::acceptOcrReview,
            onDismiss = viewModel::dismissOcrReview
        )
    }

    if (uiState.isOcrRunning) {
        val progress = uiState.ocrProgress
        val progressMessage = if (progress != null) {
            val args = com.docufind.app.ocr.OcrResultMapper.progressMessageArgs(progress)
            if (args.isEmpty()) {
                context.getString(com.docufind.app.ocr.OcrResultMapper.progressMessageResId(progress))
            } else {
                context.getString(
                    com.docufind.app.ocr.OcrResultMapper.progressMessageResId(progress),
                    *args
                )
            }
        } else {
            stringResource(R.string.ocr_progress_preparing)
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.ocr_running_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = progressMessage,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::cancelOcr) {
                    Text(stringResource(R.string.ocr_cancel))
                }
            }
        )
    }

    if (uiState.showPinSetupGate) {
        Box(modifier = Modifier.fillMaxSize()) {
            SecuritySetupFlow(onComplete = viewModel::onPinSetupComplete)
        }
        return
    }

    if (uiState.showExpiryBeforeIssueDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExpiryBeforeIssueDialog,
            title = { Text(stringResource(R.string.expiry_before_issue_title)) },
            text = { Text(stringResource(R.string.expiry_before_issue_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmExpiryBeforeIssue) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExpiryBeforeIssueDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateDialog,
            title = { Text(stringResource(R.string.duplicate_record_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.duplicate_record_message,
                        uiState.duplicateTitles.joinToString("\n")
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSaveDespiteDuplicate) {
                    Text(stringResource(R.string.duplicate_save_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicateDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmDialog,
            title = { Text(stringResource(R.string.confirm_save_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.confirm_save_message))
                    Text(
                        text = stringResource(R.string.confirm_save_files_count, uiState.attachments.size),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    uiState.attachments.forEach { entry ->
                        FilePreviewCard(
                            fileName = entry.attachment.displayName,
                            fileSizeLabel = AttachmentHelper.formatFileSize(entry.attachment.sizeBytes),
                            previewPath = entry.attachment.localPreviewPath,
                            mimeType = entry.attachment.mimeType,
                            onRemove = {},
                            showRemove = false,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSave) {
                    Text(stringResource(R.string.confirm_save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmDialog) {
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
                        text = stringResource(R.string.new_document_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            if (!uiState.isSaving) {
                androidx.compose.material3.Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.save_securely),
                        onClick = viewModel::onSaveClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AddSourceOptionsRow(
                    onScanClick = {
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED -> launchCamera()
                            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onFileClick = { openFilePicker() }
                )
            }

            item { TagItTwoWaysSection() }

            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.field_document_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                DocuFindCategoryPicker(
                    selectedId = uiState.categoryId,
                    onSelected = viewModel::onCategoryChange
                )
            }

            if (uiState.subCategoryOptions.isNotEmpty()) {
                item {
                    DocuFindOptionPicker(
                        label = stringResource(R.string.field_sub_category),
                        options = uiState.subCategoryOptions,
                        selected = uiState.subCategory,
                        onSelected = viewModel::onSubCategoryChange,
                        leadingIcon = Icons.Default.Label
                    )
                }
            }

            if (uiState.categoryId.isNotBlank()) {
                item {
                    CategoryFieldsForm(
                        categoryId = uiState.categoryId,
                        values = uiState.categoryFieldValues,
                        onValueChange = viewModel::onCategoryFieldChange
                    )
                }
            }

            item {
                OptionalDropdown(
                    label = stringResource(R.string.field_family_member),
                    options = listOf(null to stringResource(R.string.none)) +
                        uiState.familyMembers.map { it.id to it.name },
                    selectedId = uiState.familyMemberId,
                    onSelected = viewModel::onFamilyMemberChange
                )
            }

            item {
                OptionalDropdown(
                    label = stringResource(R.string.field_pet),
                    options = listOf(null to stringResource(R.string.none)) +
                        uiState.pets.map { it.id to it.name },
                    selectedId = uiState.petId,
                    onSelected = viewModel::onPetChange
                )
            }

            if (uiState.showIssueDate) {
                item {
                    DocuFindDateField(
                        label = CategoryFieldRegistry.issueDateLabel(uiState.categoryId),
                        epochMillis = uiState.issueDate,
                        onDateSelected = viewModel::onIssueDateChange,
                        allowClear = true
                    )
                }
            }

            if (uiState.showExpiryDate) {
                item {
                    DocuFindDateField(
                        label = CategoryFieldRegistry.expiryDateLabel(uiState.categoryId),
                        epochMillis = uiState.expiryDate,
                        onDateSelected = viewModel::onExpiryDateChange,
                        allowClear = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text(stringResource(R.string.field_notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.tagsText,
                    onValueChange = viewModel::onTagsChange,
                    label = { Text(stringResource(R.string.field_tags)) },
                    placeholder = { Text(stringResource(R.string.field_tags_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ReminderToggleRow(
                    enabled = uiState.reminderEnabled,
                    onToggle = viewModel::onReminderToggle,
                    defaultReminderTimeMinutes = uiState.defaultReminderTimeMinutes,
                    expiryDateMillis = uiState.expiryDate
                )
            }

            item {
                AttachmentListSection(
                    attachments = uiState.attachments,
                    onRemove = viewModel::removeAttachment,
                    onAddClick = { openFilePicker() }
                )
            }

            item {
                if (uiState.isSaving) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.saving_document),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubCategoryDropdown(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_sub_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalDropdown(
    label: String,
    options: List<Pair<String?, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedId }?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    defaultReminderTimeMinutes: Int,
    expiryDateMillis: Long?
) {
    val context = LocalContext.current
    val use24Hour = DateFormat.is24HourFormat(context)
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.field_reminder),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.field_reminder_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (enabled) {
                Text(
                    text = stringResource(
                        R.string.reminder_document_schedule_hint,
                        ReminderDateTimeFormatter.formatTime(defaultReminderTimeMinutes, use24Hour)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                expiryDateMillis?.let { expiry ->
                    val firstTrigger = ReminderTriggerCalculator.triggerBeforeDueDate(
                        expiry,
                        ReminderScheduleDefaults.OFFSET_DAYS_BEFORE.first(),
                        defaultReminderTimeMinutes
                    )
                    Text(
                        text = stringResource(
                            R.string.reminder_document_first_alert,
                            ReminderDateTimeFormatter.formatDateTime(firstTrigger, use24Hour)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
