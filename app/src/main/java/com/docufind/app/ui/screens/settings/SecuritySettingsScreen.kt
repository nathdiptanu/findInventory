package com.docufind.app.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.security.settings.AutoLockTimeout
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.SettingsListItem
import com.docufind.app.ui.screens.setup.PinSetupScreen
import com.docufind.app.ui.util.rememberFragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    ForceSecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = rememberFragmentActivity()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var showChangePin by remember { mutableStateOf(false) }
    var autoLockExpanded by remember { mutableStateOf(false) }

    if (showChangePin) {
        Box(modifier = Modifier.fillMaxSize()) {
            PinSetupScreen(onComplete = { showChangePin = false })
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_centre_title), fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.security_encryption_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.security_encryption_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(
                        title = stringResource(R.string.security_change_pin),
                        icon = Icons.Default.Lock,
                        onClick = { showChangePin = true }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.use_biometrics)) },
                        supportingContent = { Text(stringResource(R.string.biometric_setup_subtitle)) },
                        leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    val act = activity ?: return@Switch
                                    scope.launch {
                                        if (enabled) {
                                            when (val result = viewModel.enrollBiometric(act)) {
                                                is AuthResult.Success -> viewModel.setBiometricEnabled(true)
                                                is AuthResult.Error -> message = result.message
                                                AuthResult.Cancelled -> Unit
                                                AuthResult.Failed ->
                                                    message = context.getString(R.string.biometric_setup_failed)
                                            }
                                        } else {
                                            viewModel.setBiometricEnabled(false)
                                        }
                                    }
                                },
                                enabled = uiState.biometricAvailable && activity != null && uiState.pinConfigured
                            )
                        }
                    )
                    ExposedDropdownMenuBox(
                        expanded = autoLockExpanded,
                        onExpandedChange = { autoLockExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = autoLockLabel(uiState.autoLockTimeout),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.security_auto_lock)) },
                            leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(autoLockExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = autoLockExpanded,
                            onDismissRequest = { autoLockExpanded = false }
                        ) {
                            AutoLockTimeout.entries.forEach { timeout ->
                                DropdownMenuItem(
                                    text = { Text(autoLockLabel(timeout)) },
                                    onClick = {
                                        viewModel.setAutoLockTimeout(timeout)
                                        autoLockExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.security_screenshot_protection)) },
                        supportingContent = { Text(stringResource(R.string.security_screenshot_protection_desc)) },
                        leadingContent = { Icon(Icons.Default.ScreenshotMonitor, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = !uiState.allowScreenshots,
                                onCheckedChange = { protected ->
                                    viewModel.setAllowScreenshots(allowed = !protected)
                                }
                            )
                        }
                    )
                }
            }
            if (!uiState.pinConfigured) {
                Text(
                    text = stringResource(R.string.security_pin_not_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.security_local_storage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.security_limitations_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.security_limitations_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun autoLockLabel(timeout: AutoLockTimeout): String = when (timeout) {
    AutoLockTimeout.SEC_30 -> stringResource(R.string.security_auto_lock_30s)
    AutoLockTimeout.MIN_1 -> stringResource(R.string.security_auto_lock_1m)
    AutoLockTimeout.MIN_2 -> stringResource(R.string.security_auto_lock_2m)
    AutoLockTimeout.MIN_5 -> stringResource(R.string.security_auto_lock_5m)
    AutoLockTimeout.ALWAYS -> stringResource(R.string.security_auto_lock_always)
}
