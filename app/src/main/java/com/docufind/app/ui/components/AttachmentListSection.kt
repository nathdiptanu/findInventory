package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.screens.add.AttachmentHelper
import com.docufind.app.domain.model.PendingAttachmentEntry

@Composable
fun AttachmentListSection(
    attachments: List<PendingAttachmentEntry>,
    onRemove: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocuFindCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.attachments_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.attachments_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                attachments.forEach { entry ->
                    FilePreviewCard(
                        fileName = entry.attachment.displayName,
                        fileSizeLabel = AttachmentHelper.formatFileSize(entry.attachment.sizeBytes),
                        previewPath = entry.attachment.localPreviewPath,
                        mimeType = entry.attachment.mimeType,
                        onRemove = { onRemove(entry.id) }
                    )
                }
            }
            TextButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.add_another_file))
            }
        }
    }
}
