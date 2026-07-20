package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.export.pdf.PdfExportPasswordValidator
import com.docufind.app.security.crypto.SecureMemory

@Composable
fun ExportPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.export_pdf_password_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.export_pdf_password_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.export_pdf_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.export_pdf_confirm_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
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
                }
            ) {
                Text(stringResource(R.string.export_pdf_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    SecureMemory.wipe(password.toCharArray())
                    SecureMemory.wipe(confirmPassword.toCharArray())
                    password = ""
                    confirmPassword = ""
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
