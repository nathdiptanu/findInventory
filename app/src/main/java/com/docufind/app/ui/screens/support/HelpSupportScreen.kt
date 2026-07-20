package com.docufind.app.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.docufind.app.R
import com.docufind.app.support.SupportConstants
import com.docufind.app.support.SupportEmailResult
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.SettingsListItem

@Composable
fun CopyReportDialog(
    reportText: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_no_email_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.support_no_email_message),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = reportText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.support_copy_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private data class HelpSupportEntry(
    val titleRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val showChevron: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    onReportBug: () -> Unit,
    onSendFeedback: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: HelpSupportViewModel = hiltViewModel()
) {
    var showFaqDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    var copyReportText by remember { mutableStateOf<String?>(null) }

    val supportItems = listOf(
        HelpSupportEntry(R.string.support_faq, Icons.Default.QuestionAnswer, { showFaqDialog = true }),
        HelpSupportEntry(R.string.settings_report_bug, Icons.Default.BugReport, onReportBug),
        HelpSupportEntry(R.string.settings_send_feedback, Icons.Default.Feedback, onSendFeedback),
        HelpSupportEntry(R.string.support_contact, Icons.Default.Email, {
            when (val result = viewModel.contactSupport()) {
                is SupportEmailResult.Launched -> Unit
                is SupportEmailResult.NoEmailApp -> copyReportText = result.reportText
            }
        }),
        HelpSupportEntry(R.string.settings_privacy_policy, Icons.Default.Policy, onPrivacy),
        HelpSupportEntry(
            titleRes = R.string.support_app_version,
            icon = Icons.Default.Info,
            onClick = {},
            showChevron = false
        ),
        HelpSupportEntry(R.string.support_device_info, Icons.Default.PhoneAndroid, { showDeviceInfoDialog = true })
    )

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text(stringResource(R.string.support_faq)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FaqBlock(
                        question = stringResource(R.string.support_faq_q1),
                        answer = stringResource(R.string.support_faq_a1)
                    )
                    FaqBlock(
                        question = stringResource(R.string.support_faq_q2),
                        answer = stringResource(R.string.support_faq_a2)
                    )
                    FaqBlock(
                        question = stringResource(R.string.support_faq_q3),
                        answer = stringResource(R.string.support_faq_a3)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }

    if (showDeviceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceInfoDialog = false },
            title = { Text(stringResource(R.string.support_device_info)) },
            text = {
                Text(
                    text = viewModel.diagnostics.toReportBlock(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.copyTextToClipboard(viewModel.diagnostics.toReportBlock())
                    showDeviceInfoDialog = false
                }) {
                    Text(stringResource(R.string.support_copy_report))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceInfoDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    copyReportText?.let { text ->
        CopyReportDialog(
            reportText = text,
            onDismiss = { copyReportText = null },
            onCopy = {
                viewModel.copyTextToClipboard(text)
                copyReportText = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_help), fontWeight = FontWeight.Bold) },
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
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.support_help_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SupportConstants.SUPPORT_EMAIL,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    supportItems.forEach { item ->
                        if (item.titleRes == R.string.support_app_version) {
                            ListItem(
                                headlineContent = { Text(stringResource(item.titleRes)) },
                                supportingContent = { Text(viewModel.appVersion) },
                                leadingContent = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        } else {
                            SettingsListItem(
                                title = stringResource(item.titleRes),
                                icon = item.icon,
                                onClick = item.onClick,
                                showChevron = item.showChevron
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqBlock(question: String, answer: String) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
