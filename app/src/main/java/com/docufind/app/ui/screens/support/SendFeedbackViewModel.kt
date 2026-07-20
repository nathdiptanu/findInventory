package com.docufind.app.ui.screens.support

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

data class SendFeedbackUiState(
    val message: String = "",
    val errorMessage: String? = null,
    val copyReportText: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class SendFeedbackViewModel @Inject constructor(
    private val supportEmailSender: SupportEmailSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendFeedbackUiState())
    val uiState: StateFlow<SendFeedbackUiState> = _uiState.asStateFlow()

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun dismissCopyDialog() = _uiState.update { it.copy(copyReportText = null) }

    fun submit() {
        val message = _uiState.value.message.trim()
        if (message.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your feedback.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val body = supportEmailSender.buildFeedbackBody(message)
            when (
                val result = supportEmailSender.sendEmail(
                    subject = SupportConstants.FEEDBACK_SUBJECT,
                    body = body
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

    fun copyReportToClipboard() {
        _uiState.value.copyReportText?.let { supportEmailSender.copyToClipboard(it) }
    }
}
