package com.docufind.app.ui.screens.reminders

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.domain.model.reminder.ReminderImportance
import com.docufind.app.domain.model.reminder.ReminderListItem
import com.docufind.app.domain.model.reminder.ReminderTab
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindEmptyState
import com.docufind.app.ui.components.ReminderTimePickerDialog
import com.docufind.app.ui.components.profile.ProfileEmptyState
import com.docufind.app.reminder.ReminderDateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onReminderClick: (ReminderListItem) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val permissionDeniedMessage = stringResource(R.string.reminder_test_permission_denied)
    val testScheduledMessage = stringResource(R.string.reminder_test_scheduled)
    val defaultTimeSavedMessage = stringResource(R.string.reminder_default_time_saved)

    LaunchedEffect(uiState.infoMessage) {
        when (uiState.infoMessage) {
            "permission_disabled" -> {
                snackbarHostState.showSnackbar(permissionDeniedMessage)
                viewModel.clearInfoMessage()
            }
            "test_scheduled" -> {
                snackbarHostState.showSnackbar(testScheduledMessage)
                viewModel.clearInfoMessage()
            }
            "default_time_saved" -> {
                snackbarHostState.showSnackbar(defaultTimeSavedMessage)
                viewModel.clearInfoMessage()
            }
        }
    }

    if (!uiState.notificationsEnabled && uiState.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionRationale,
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            title = { Text(stringResource(R.string.reminder_permission_title)) },
            text = { Text(stringResource(R.string.reminder_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionRationale()
                    onRequestNotificationPermission()
                }) {
                    Text(stringResource(R.string.reminder_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPermissionRationale) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showAddDialog) {
        CustomReminderDialog(
            onDismiss = viewModel::dismissAddDialog,
            onSave = viewModel::saveCustomReminder
        )
    }

    uiState.editingReminder?.let { reminder ->
        CustomReminderDialog(
            existing = reminder,
            onDismiss = viewModel::dismissEditDialog,
            onSave = viewModel::updateReminder
        )
    }

    if (uiState.showDefaultTimeDialog) {
        DefaultReminderTimeDialog(
            currentMinutes = uiState.defaultReminderTimeMinutes,
            onDismiss = viewModel::dismissDefaultTimeDialog,
            onSave = viewModel::updateDefaultReminderTime
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title), fontWeight = FontWeight.Bold) },
                actions = {
                    if (!uiState.notificationsEnabled) {
                        IconButton(onClick = viewModel::showPermissionRationale) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.notificationsEnabled) {
                DocuFindCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = viewModel::showPermissionRationale
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Text(
                            text = stringResource(R.string.reminder_permission_banner),
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (!uiState.exactAlarmsAvailable) {
                DocuFindCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Text(
                            text = stringResource(R.string.reminder_exact_alarm_banner),
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = viewModel::sendTestNotification,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Text(
                    text = stringResource(R.string.reminder_test_notification),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            DocuFindCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = viewModel::showDefaultTimeDialog
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_default_time_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.reminder_default_time_desc,
                                formatReminderTime(uiState.defaultReminderTimeMinutes)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.reminder_default_time_change),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReminderTab.entries) { tab ->
                    FilterChip(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.reminders.isEmpty()) {
                    item {
                        ProfileEmptyState(message = stringResource(R.string.reminders_empty))
                    }
                } else if (uiState.filteredReminders.isEmpty()) {
                    item {
                        DocuFindEmptyState(message = stringResource(R.string.reminders_tab_empty))
                    }
                } else {
                    items(uiState.filteredReminders, key = { it.id }) { reminder ->
                        ReminderListCard(
                            reminder = reminder,
                            onClick = { viewModel.showEditDialog(reminder) },
                            onComplete = { viewModel.markCompleted(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListCard(
    reminder: ReminderListItem,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    DocuFindCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${reminder.title} · ${reminder.category.displayName}",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${formatReminderDateTime(reminder.triggerAt)} · ${reminder.repeatType.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = importanceColor(reminder.importance),
                    maxLines = 1
                )
            }
            if (reminder.status.name == "ACTIVE") {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.mark_complete))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultReminderTimeDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    ReminderTimePickerDialog(
        initialMinutes = currentMinutes,
        use24Hour = use24Hour,
        onDismiss = onDismiss,
        onConfirm = onSave
    )
}

private fun formatReminderTime(minutes: Int): String {
    return ReminderDateTimeFormatter.formatTime(minutes, use24Hour = false)
}

@Composable
private fun importanceColor(importance: ReminderImportance) = when (importance) {
    ReminderImportance.URGENT -> MaterialTheme.colorScheme.error
    ReminderImportance.IMPORTANT -> MaterialTheme.colorScheme.primary
    ReminderImportance.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatReminderDateTime(epochMillis: Long): String {
    return ReminderDateTimeFormatter.formatDateTime(epochMillis, use24Hour = false)
}
