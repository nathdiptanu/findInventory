package com.docufind.app.ui.screens.support

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.support.SupportConstants
import com.docufind.app.support.SupportEmailResult
import com.docufind.app.support.SupportEmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportBugUiState(
    val issueTitle: String = "",
    val description: String = "",
    val moduleAffected: String = "General",
    val stepsToReproduce: String = "",
    val attachmentUri: Uri? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val errorMessage: String? = null,
    val copyReportText: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class ReportBugViewModel @Inject constructor(
    private val supportEmailSender: SupportEmailSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportBugUiState())
    val uiState: StateFlow<ReportBugUiState> = _uiState.asStateFlow()

    fun onIssueTitleChange(value: String) = _uiState.update { it.copy(issueTitle = value) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onModuleChange(value: String) = _uiState.update { it.copy(moduleAffected = value) }

    fun onStepsChange(value: String) = _uiState.update { it.copy(stepsToReproduce = value) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun dismissCopyDialog() = _uiState.update { it.copy(copyReportText = null) }

    fun onAttachmentSelected(uri: Uri, displayName: String, mimeType: String) {
        val size = supportEmailSender.queryAttachmentSize(uri)
        if (size > SupportConstants.MAX_ATTACHMENT_BYTES) {
            _uiState.update {
                it.copy(
                    errorMessage = "Attachment is larger than 10 MB. Please attach a smaller image.",
                    attachmentUri = null,
                    attachmentName = null,
                    attachmentMimeType = null
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                attachmentUri = uri,
                attachmentName = displayName,
                attachmentMimeType = mimeType,
                errorMessage = null
            )
        }
    }

    fun removeAttachment() = _uiState.update {
        it.copy(attachmentUri = null, attachmentName = null, attachmentMimeType = null)
    }

    fun submit() {
        val state = _uiState.value
        when {
            state.issueTitle.isBlank() ->
                _uiState.update { it.copy(errorMessage = "Issue title is required.") }
            state.description.isBlank() ->
                _uiState.update { it.copy(errorMessage = "Description is required.") }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                val body = supportEmailSender.buildBugReportBody(
                    issueTitle = state.issueTitle.trim(),
                    description = state.description.trim(),
                    moduleAffected = state.moduleAffected,
                    stepsToReproduce = state.stepsToReproduce.trim().ifBlank { "N/A" }
                )
                val attachmentUri = state.attachmentUri?.let { uri ->
                    supportEmailSender.prepareAttachmentFromUri(
                        uri,
                        state.attachmentName ?: "screenshot.jpg"
                    ).getOrNull()
                }
                when (
                    val result = supportEmailSender.sendEmail(
                        subject = SupportConstants.BUG_REPORT_SUBJECT,
                        body = body,
                        attachmentUri = attachmentUri,
                        attachmentMimeType = state.attachmentMimeType
                    )
                ) {
                    SupportEmailResult.Launched -> _uiState.update {
                        it.copy(isSubmitting = false, copyReportText = null)
                    }
                    is SupportEmailResult.NoEmailApp -> _uiState.update {
                        it.copy(isSubmitting = false, copyReportText = result.reportText)
                    }
                }
            }
        }
    }

    fun copyReportToClipboard() {
        _uiState.value.copyReportText?.let { supportEmailSender.copyToClipboard(it) }
    }
}
