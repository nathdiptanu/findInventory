package com.docufind.app.ui.screens.support

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.ui.components.form.DocuFindFormScaffold
import com.docufind.app.ui.components.form.DocuFindFormSection

@Composable
fun SendFeedbackScreen(
    onBack: () -> Unit,
    viewModel: SendFeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
        title = stringResource(R.string.settings_send_feedback),
        onBack = onBack,
        onSave = viewModel::submit,
        saveEnabled = !uiState.isSubmitting,
        saveLabel = stringResource(R.string.feedback_submit),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
        }

        DocuFindFormSection {
            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::onMessageChange,
                label = { Text(stringResource(R.string.feedback_message)) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
