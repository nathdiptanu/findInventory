package com.docufind.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.docufind.app.R
import com.docufind.app.export.pdf.PdfExportPasswordValidator
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.export.EXPORT_WARNING_MESSAGE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPasswordScreen(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            topBar = {
                DocuFindTopBar(
                    title = stringResource(R.string.export_pdf_password_title),
                    onBack = onDismiss
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocuFindPrimaryButton(
                        text = stringResource(R.string.export_pdf_confirm),
                        onClick = {
                            val pwd = password.toCharArray()
                            val confirm = confirmPassword.toCharArray()
                            when (val validation = PdfExportPasswordValidator.validate(pwd)) {
                                is PdfExportPasswordValidator.ValidationResult.Invalid -> {
                                    errorMessage = validation.message
                                    SecureMemory.wipe(pwd)
                                    SecureMemory.wipe(confirm)
                                }
                                PdfExportPasswordValidator.ValidationResult.Valid -> {
                                    if (!PdfExportPasswordValidator.passwordsMatch(pwd, confirm)) {
                                        errorMessage = "Passwords do not match."
                                        SecureMemory.wipe(pwd)
                                        SecureMemory.wipe(confirm)
                                    } else {
                                        SecureMemory.wipe(confirm)
                                        password = ""
                                        confirmPassword = ""
                                        errorMessage = null
                                        onConfirm(pwd)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_pdf_password_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.export_pdf_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.export_pdf_confirm_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportConfirmationScreen(
    onConfirm: (includeOcr: Boolean) -> Unit,
    onDismiss: () -> Unit,
    showOcrOption: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            topBar = {
                DocuFindTopBar(
                    title = stringResource(R.string.export_pdf),
                    onBack = onDismiss
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showOcrOption) {
                        DocuFindPrimaryButton(
                            text = "Continue with OCR",
                            onClick = { onConfirm(true) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DocuFindPrimaryButton(
                            text = "Continue without OCR",
                            onClick = { onConfirm(false) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        DocuFindPrimaryButton(
                            text = stringResource(R.string.continue_action),
                            onClick = { onConfirm(false) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = EXPORT_WARNING_MESSAGE,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showOcrOption) {
                    Text(
                        text = "OCR text from images can be included when available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExportPasswordDialog(onConfirm: (CharArray) -> Unit, onDismiss: () -> Unit) =
    ExportPasswordScreen(onConfirm = onConfirm, onDismiss = onDismiss)

@Composable
fun ExportConfirmationDialog(
    onConfirm: (includeOcr: Boolean) -> Unit,
    onDismiss: () -> Unit,
    showOcrOption: Boolean = false
) = ExportConfirmationScreen(onConfirm = onConfirm, onDismiss = onDismiss, showOcrOption = showOcrOption)
