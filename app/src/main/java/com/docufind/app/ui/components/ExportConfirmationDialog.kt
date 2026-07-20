package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docufind.app.security.export.EXPORT_WARNING_MESSAGE

@Composable
fun ExportConfirmationDialog(
    onConfirm: (includeOcr: Boolean) -> Unit,
    onDismiss: () -> Unit,
    showOcrOption: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Export as PDF", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = EXPORT_WARNING_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (showOcrOption) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OCR text from images can be included when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (showOcrOption) {
                Column {
                    TextButton(onClick = { onConfirm(true) }) {
                        Text("Continue with OCR")
                    }
                    TextButton(onClick = { onConfirm(false) }) {
                        Text("Continue without OCR")
                    }
                }
            } else {
                TextButton(onClick = { onConfirm(false) }) {
                    Text("Continue")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
