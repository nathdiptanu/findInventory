package com.docufind.app.ui.screens.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.backup.BackupStatus
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.screens.add.AttachmentHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val isEmpty = storageInfo.recordCount == 0 && storageInfo.fileCount == 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_storage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEmpty) {
                DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.storage_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.storage_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            StorageStatCard(
                label = stringResource(R.string.storage_total_used),
                value = AttachmentHelper.formatFileSize(storageInfo.totalBytes)
            )
            StorageStatCard(
                label = stringResource(R.string.storage_document_count),
                value = storageInfo.recordCount.toString()
            )
            StorageStatCard(
                label = stringResource(R.string.storage_file_count),
                value = storageInfo.fileCount.toString()
            )
            StorageStatCard(
                label = stringResource(R.string.storage_last_backup),
                value = storageInfo.lastBackupAt?.let { formatDate(it) }
                    ?: stringResource(R.string.storage_never)
            )
            StorageStatCard(
                label = stringResource(R.string.storage_backup_status),
                value = backupStatusLabel(storageInfo.backupStatus)
            )
        }
    }
}

@Composable
private fun StorageStatCard(label: String, value: String) {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun backupStatusLabel(status: BackupStatus): String = when (status) {
    BackupStatus.NEVER -> stringResource(R.string.storage_backup_never)
    BackupStatus.SUCCESS -> stringResource(R.string.storage_backup_ok)
    BackupStatus.FAILED -> stringResource(R.string.storage_backup_failed)
}

private fun formatDate(epoch: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatter)
}
