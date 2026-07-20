package com.docufind.app.ui.screens.module

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.SaveDocumentResult
import com.docufind.app.domain.model.module.ModuleRecordDetail
import com.docufind.app.domain.model.module.ModuleRecordUpdate
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.export.pdf.PdfExportRequest
import com.docufind.app.export.pdf.PdfExportResult
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.file.SecureFileAccessManager
import com.docufind.app.security.file.SecureFileAccessResult
import com.docufind.app.security.pin.PinManager
import com.docufind.app.security.session.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModuleDetailUiState(
    val detail: ModuleRecordDetail? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editTitle: String = "",
    val editNotes: String = "",
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val exportedFile: File? = null,
    val showUnlockDialog: Boolean = false,
    val showExportConfirmation: Boolean = false,
    val pendingFileId: String? = null,
    val pendingFileName: String? = null,
    val pendingMimeType: String? = null,
    val pendingFileOp: FileOp? = null,
    val unlockError: String? = null,
    val requiresVaultUnlock: Boolean = false,
    val showExportConfirmDialog: Boolean = false,
    val showExportPasswordDialog: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportIncludeOcr: Boolean = false,
    val pendingExportPin: CharArray? = null
)

enum class FileOp { SHARE, DOWNLOAD }

@HiltViewModel
class ModuleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VaultRecordRepository,
    private val secureFileAccessManager: SecureFileAccessManager,
    private val vaultSessionManager: VaultSessionManager,
    private val pinManager: PinManager
) : ViewModel() {

    private val recordId: String = savedStateHandle.get<String>("recordId").orEmpty()

    val recordIdForUnlock: String get() = recordId

    private val _uiState = MutableStateFlow(ModuleDetailUiState())
    val uiState: StateFlow<ModuleDetailUiState> = _uiState.asStateFlow()

    val detailFlow: StateFlow<ModuleRecordDetail?> = repository
        .observeRecordDetail(recordId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (recordId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, notFound = true) }
        } else if (pinManager.hasPinConfigured() && !vaultSessionManager.isUnlocked.value) {
            _uiState.update { it.copy(isLoading = false, requiresVaultUnlock = true) }
        } else {
            viewModelScope.launch {
                detailFlow.collect { detail ->
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            editTitle = detail?.title ?: it.editTitle,
                            editNotes = detail?.notes.orEmpty(),
                            isLoading = false,
                            notFound = detail == null
                        )
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            runCatching {
                repository.setFavorite(detail.id, !detail.isFavorite)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not update favourite.")
                }
            }
        }
    }

    fun showDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = true) }

    fun dismissDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun showEditDialog() = _uiState.update {
        it.copy(
            showEditDialog = true,
            editTitle = it.detail?.title.orEmpty(),
            editNotes = it.detail?.notes.orEmpty()
        )
    }

    fun dismissEditDialog() = _uiState.update { it.copy(showEditDialog = false) }

    fun onEditTitleChange(value: String) = _uiState.update { it.copy(editTitle = value) }

    fun onEditNotesChange(value: String) = _uiState.update { it.copy(editNotes = value) }

    fun saveEdit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val detail = state.detail ?: return
        viewModelScope.launch {
            runCatching {
                repository.updateRecord(
                    ModuleRecordUpdate(
                        recordId = detail.id,
                        title = state.editTitle.trim(),
                        subCategory = detail.subCategory,
                        notes = state.editNotes,
                        issueDate = detail.issueDate,
                        expiryDate = detail.expiryDate
                    )
                )
            }.fold(
                onSuccess = { result ->
                    when (result) {
                        is SaveDocumentResult.Success -> {
                            _uiState.update { it.copy(showEditDialog = false) }
                            onSuccess()
                        }
                        is SaveDocumentResult.Error -> {
                            _uiState.update { it.copy(errorMessage = result.message) }
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Could not update this record.")
                    }
                }
            )
        }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }
            runCatching { repository.deleteRecord(recordId) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    onDeleted()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.message ?: "Could not delete this record."
                        )
                    }
                }
        }
    }

    fun requestFileOp(fileId: String, mimeType: String, op: FileOp) {
        _uiState.update {
            it.copy(
                pendingFileId = fileId,
                pendingMimeType = mimeType,
                pendingFileOp = op,
                showExportConfirmation = true,
                unlockError = null
            )
        }
    }

    fun dismissExportConfirmation() {
        _uiState.update {
            it.copy(
                showExportConfirmation = false,
                pendingFileId = null,
                pendingFileOp = null
            )
        }
    }

    fun confirmExport(
        activity: androidx.fragment.app.FragmentActivity,
        onShare: (java.io.File, String) -> Unit,
        onDownloaded: (String) -> Unit
    ) {
        val state = _uiState.value
        val fileId = state.pendingFileId ?: return
        val op = state.pendingFileOp ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showExportConfirmation = false) }
            runExportDecrypt(activity, fileId, op, pin = null, onShare, onDownloaded)
        }
    }

    fun dismissUnlockDialog() {
        _uiState.update {
            val pendingPin = it.pendingExportPin
            if (pendingPin != null) SecureMemory.wipe(pendingPin)
            it.copy(
                showUnlockDialog = false,
                pendingFileId = null,
                pendingFileOp = null,
                pendingExportPin = null,
                unlockError = null
            )
        }
    }

    fun performPendingFileOp(
        activity: androidx.fragment.app.FragmentActivity,
        pin: CharArray? = null,
        onShare: (java.io.File, String) -> Unit,
        onDownloaded: (String) -> Unit
    ) {
        val state = _uiState.value
        val fileId = state.pendingFileId ?: return
        val op = state.pendingFileOp ?: return
        viewModelScope.launch {
            runExportDecrypt(activity, fileId, op, pin, onShare, onDownloaded)
        }
    }

    fun tryFileOpOrUnlock(
        fileId: String,
        mimeType: String,
        op: FileOp
    ) {
        requestFileOp(fileId, mimeType, op)
    }

    private suspend fun runExportDecrypt(
        activity: androidx.fragment.app.FragmentActivity,
        fileId: String,
        op: FileOp,
        pin: CharArray?,
        onShare: (java.io.File, String) -> Unit,
        onDownloaded: (String) -> Unit
    ) {
        runCatching { secureFileAccessManager.decryptFileForExport(activity, fileId, pin) }
            .onSuccess { result ->
                when (result) {
                    is SecureFileAccessResult.Ready -> {
                        _uiState.update {
                            it.copy(
                                showUnlockDialog = false,
                                pendingFileId = null,
                                pendingFileOp = null,
                                exportedFile = result.file,
                                unlockError = null
                            )
                        }
                        when (op) {
                            FileOp.SHARE -> runCatching { onShare(result.file, result.mimeType) }
                                .onFailure {
                                    _uiState.update { state -> state.copy(errorMessage = "Could not share file.") }
                                }
                            FileOp.DOWNLOAD -> {
                                secureFileAccessManager.exportToDownloads(result.file, result.displayName)
                                    .onSuccess { saved -> onDownloaded(saved.absolutePath) }
                                    .onFailure {
                                        _uiState.update { it.copy(errorMessage = "Could not export file.") }
                                    }
                            }
                        }
                    }
                    is SecureFileAccessResult.AuthRequired -> {
                        _uiState.update { it.copy(showUnlockDialog = true, unlockError = result.message) }
                    }
                    is SecureFileAccessResult.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.message,
                                showUnlockDialog = false,
                                pendingFileId = null,
                                pendingFileOp = null
                            )
                        }
                    }
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Could not export file.",
                        showUnlockDialog = false,
                        pendingFileId = null,
                        pendingFileOp = null
                    )
                }
            }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }

    fun requestExportPdf() = _uiState.update { it.copy(showExportConfirmDialog = true) }

    fun dismissExportConfirmDialog() = _uiState.update { it.copy(showExportConfirmDialog = false) }

    fun confirmExportPdf(includeOcr: Boolean) = _uiState.update {
        it.copy(showExportConfirmDialog = false, showExportPasswordDialog = true, exportIncludeOcr = includeOcr)
    }

    fun dismissExportPasswordDialog() = _uiState.update { it.copy(showExportPasswordDialog = false) }

    fun performPdfExport(
        activity: androidx.fragment.app.FragmentActivity,
        exportPassword: CharArray,
        pin: CharArray? = null,
        onShare: (java.io.File) -> Unit
    ) {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true, showExportPasswordDialog = false) }
            val request = PdfExportRequest.Record(
                recordId = detail.id,
                includeOcr = _uiState.value.exportIncludeOcr,
                exportPassword = exportPassword.copyOf()
            )
            try {
                when (val result = secureFileAccessManager.exportPdf(activity, request, pin)) {
                    is PdfExportResult.Ready -> {
                        _uiState.update {
                            it.copy(isExportingPdf = false, actionMessage = "Secure PDF ready to share.")
                        }
                        onShare(result.file)
                    }
                    is PdfExportResult.AuthRequired -> {
                        _uiState.update {
                            it.copy(
                                isExportingPdf = false,
                                showUnlockDialog = true,
                                pendingExportPin = exportPassword.copyOf(),
                                unlockError = result.message,
                                pendingFileOp = null,
                                pendingFileId = null
                            )
                        }
                    }
                    is PdfExportResult.Error -> {
                        _uiState.update {
                            it.copy(isExportingPdf = false, errorMessage = result.message)
                        }
                    }
                }
            } finally {
                SecureMemory.wipe(exportPassword)
                SecureMemory.wipe(request.exportPassword)
            }
        }
    }

    fun retryPdfExportAfterUnlock(
        activity: androidx.fragment.app.FragmentActivity,
        pin: CharArray?,
        onShare: (java.io.File) -> Unit
    ) {
        val pendingPassword = _uiState.value.pendingExportPin ?: return
        _uiState.update { it.copy(pendingExportPin = null, showUnlockDialog = false) }
        performPdfExport(activity, pendingPassword, pin, onShare)
    }
}
