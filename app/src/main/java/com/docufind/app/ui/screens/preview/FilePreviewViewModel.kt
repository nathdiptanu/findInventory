package com.docufind.app.ui.screens.preview

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.domain.model.VaultCategory
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.export.pdf.PdfExportRequest
import com.docufind.app.export.pdf.PdfExportResult
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.file.SecureFileAccessManager
import com.docufind.app.security.file.SecureFileAccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PreviewMode {
    Loading,
    Pdf,
    Image,
    Unavailable,
    Missing,
    AuthRequired
}

enum class PreviewExportOp {
    SHARE,
    DOWNLOAD
}

data class FilePreviewUiState(
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val createdAt: Long = 0L,
    val categoryLabel: String = "",
    val decryptedFile: File? = null,
    val previewMode: PreviewMode = PreviewMode.Loading,
    val showUnlockDialog: Boolean = false,
    val showExportConfirmation: Boolean = false,
    val pendingExportOp: PreviewExportOp? = null,
    val showDeleteDialog: Boolean = false,
    val unlockError: String? = null,
    val actionMessage: String? = null,
    val showExportConfirmDialog: Boolean = false,
    val showExportPasswordDialog: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportIncludeOcr: Boolean = false,
    val pendingExportPassword: CharArray? = null
)

@HiltViewModel
class FilePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultFileDao: VaultFileDao,
    private val vaultRecordDao: VaultRecordDao,
    private val vaultRecordRepository: VaultRecordRepository,
    private val secureFileAccessManager: SecureFileAccessManager
) : ViewModel() {

    private val fileId: String = savedStateHandle.get<String>("fileId").orEmpty()

    private val _uiState = MutableStateFlow(FilePreviewUiState())
    val uiState: StateFlow<FilePreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val file = vaultFileDao.getById(fileId)
            if (file == null) {
                _uiState.update {
                    it.copy(
                        previewMode = PreviewMode.Missing,
                        actionMessage = "File unavailable. The original file may have been moved or deleted."
                    )
                }
            } else {
                val record = vaultRecordDao.getById(file.recordId)
                val categoryLabel = record?.category?.let { categoryId ->
                    DocuFindModule.fromId(categoryId)?.title
                        ?: VaultCategory.fromId(categoryId)?.displayName
                        ?: categoryId
                }.orEmpty()
                _uiState.update {
                    it.copy(
                        fileName = file.fileName,
                        mimeType = file.mimeType,
                        fileSize = file.fileSize,
                        createdAt = file.createdAt,
                        categoryLabel = categoryLabel
                    )
                }
            }
        }
    }

    fun loadFile(activity: FragmentActivity, pin: CharArray? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(previewMode = PreviewMode.Loading, unlockError = null) }
            when (val result = secureFileAccessManager.decryptFile(activity, fileId, pin)) {
                is SecureFileAccessResult.Ready -> applyDecrypted(result.file, result.mimeType, result.displayName)
                is SecureFileAccessResult.AuthRequired -> {
                    _uiState.update {
                        it.copy(
                            previewMode = PreviewMode.AuthRequired,
                            showUnlockDialog = true,
                            unlockError = result.message
                        )
                    }
                }
                is SecureFileAccessResult.Error -> {
                    val missing = result.message.contains("unavailable", ignoreCase = true) ||
                        result.message.contains("moved", ignoreCase = true) ||
                        result.message.contains("deleted", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            previewMode = if (missing) PreviewMode.Missing else PreviewMode.Unavailable,
                            actionMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun dismissUnlockDialog() {
        _uiState.update {
            val pendingPassword = it.pendingExportPassword
            if (pendingPassword != null) SecureMemory.wipe(pendingPassword)
            it.copy(
                showUnlockDialog = false,
                pendingExportOp = null,
                pendingExportPassword = null
            )
        }
    }

    fun requestShareExport() {
        _uiState.update { it.copy(showExportConfirmation = true, pendingExportOp = PreviewExportOp.SHARE) }
    }

    fun requestDownloadExport() {
        _uiState.update { it.copy(showExportConfirmation = true, pendingExportOp = PreviewExportOp.DOWNLOAD) }
    }

    fun dismissExportConfirmation() {
        _uiState.update { it.copy(showExportConfirmation = false, pendingExportOp = null) }
    }

    private var pendingDownloadCallback: ((String) -> Unit)? = null

    fun confirmExport(activity: FragmentActivity, onDownloaded: (String) -> Unit = {}) {
        val op = _uiState.value.pendingExportOp ?: return
        if (op == PreviewExportOp.DOWNLOAD) {
            pendingDownloadCallback = onDownloaded
        }
        viewModelScope.launch {
            _uiState.update { it.copy(showExportConfirmation = false) }
            runExport(activity, op, pin = null)
        }
    }

    fun performExportWithPin(activity: FragmentActivity, pin: CharArray, onDownloaded: (String) -> Unit = {}) {
        val op = _uiState.value.pendingExportOp ?: return
        if (op == PreviewExportOp.DOWNLOAD) {
            pendingDownloadCallback = onDownloaded
        }
        viewModelScope.launch {
            runExport(activity, op, pin)
        }
    }

    private suspend fun runExport(activity: FragmentActivity, op: PreviewExportOp, pin: CharArray?) {
        when (val result = secureFileAccessManager.decryptFileForExport(activity, fileId, pin)) {
            is SecureFileAccessResult.Ready -> {
                _uiState.update {
                    it.copy(showUnlockDialog = false, unlockError = null, pendingExportOp = null)
                }
                when (op) {
                    PreviewExportOp.SHARE -> secureFileAccessManager.shareFile(result.file, result.mimeType)
                    PreviewExportOp.DOWNLOAD -> {
                        secureFileAccessManager.exportToDownloads(result.file, _uiState.value.fileName)
                            .onSuccess { saved -> pendingDownloadCallback?.invoke(saved.absolutePath) }
                            .onFailure {
                                _uiState.update { s -> s.copy(actionMessage = "Could not export file.") }
                            }
                        pendingDownloadCallback = null
                    }
                }
            }
            is SecureFileAccessResult.AuthRequired -> {
                _uiState.update {
                    it.copy(
                        showUnlockDialog = true,
                        unlockError = result.message,
                        pendingExportOp = op
                    )
                }
            }
            is SecureFileAccessResult.Error -> {
                _uiState.update {
                    it.copy(actionMessage = result.message, pendingExportOp = null, showUnlockDialog = false)
                }
                pendingDownloadCallback = null
            }
        }
    }

    fun share(activity: FragmentActivity) {
        if (_uiState.value.decryptedFile == null) return
        requestShareExport()
    }

    fun download(onComplete: (String) -> Unit) {
        if (_uiState.value.decryptedFile == null) return
        pendingDownloadCallback = onComplete
        requestDownloadExport()
    }

    fun open() {
        val state = _uiState.value
        val file = state.decryptedFile ?: return
        secureFileAccessManager.openFile(file, state.mimeType)
            .onFailure {
                _uiState.update { s -> s.copy(actionMessage = "No app found to open this file.") }
            }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteFile(onDeleted: () -> Unit) {
        viewModelScope.launch {
            vaultRecordRepository.deleteFile(fileId).fold(
                onSuccess = {
                    _uiState.update { it.copy(showDeleteDialog = false) }
                    onDeleted()
                },
                onFailure = {
                    _uiState.update { s ->
                        s.copy(showDeleteDialog = false, actionMessage = "Could not delete file.")
                    }
                }
            )
        }
    }

    fun requestExportPdf() = _uiState.update { it.copy(showExportConfirmDialog = true) }

    fun dismissExportConfirmDialog() = _uiState.update { it.copy(showExportConfirmDialog = false) }

    fun confirmExportPdf(includeOcr: Boolean) = _uiState.update {
        it.copy(showExportConfirmDialog = false, showExportPasswordDialog = true, exportIncludeOcr = includeOcr)
    }

    fun dismissExportPasswordDialog() = _uiState.update { it.copy(showExportPasswordDialog = false) }

    fun performPdfExport(
        activity: FragmentActivity,
        exportPassword: CharArray,
        pin: CharArray? = null,
        onShare: (File) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true, showExportPasswordDialog = false) }
            val request = PdfExportRequest.SingleFile(
                fileId = fileId,
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
                                pendingExportPassword = exportPassword.copyOf(),
                                unlockError = result.message
                            )
                        }
                    }
                    is PdfExportResult.Error -> {
                        _uiState.update {
                            it.copy(isExportingPdf = false, actionMessage = result.message)
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
        activity: FragmentActivity,
        pin: CharArray?,
        onShare: (File) -> Unit
    ) {
        val pendingPassword = _uiState.value.pendingExportPassword ?: return
        _uiState.update { it.copy(pendingExportPassword = null, showUnlockDialog = false) }
        performPdfExport(activity, pendingPassword, pin, onShare)
    }

    fun shareExportedPdf(activity: FragmentActivity, file: File) {
        secureFileAccessManager.shareExportedPdf(file)
    }

    private fun applyDecrypted(file: File, mimeType: String, displayName: String) {
        val mode = when {
            mimeType == "application/pdf" || file.name.lowercase().endsWith(".pdf") -> PreviewMode.Pdf
            mimeType.startsWith("image/") -> PreviewMode.Image
            else -> PreviewMode.Unavailable
        }
        _uiState.update {
            it.copy(
                decryptedFile = file,
                mimeType = mimeType,
                fileName = displayName,
                previewMode = mode,
                showUnlockDialog = false,
                unlockError = null
            )
        }
    }
}
