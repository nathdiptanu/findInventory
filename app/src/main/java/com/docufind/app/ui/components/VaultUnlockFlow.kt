package com.docufind.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.theme.DocuFindBlue
import com.docufind.app.ui.theme.DocuFindVaultBackground
import com.docufind.app.ui.theme.DocuFindVaultDeep
import com.docufind.app.ui.theme.DocuFindVaultGlow
import com.docufind.app.ui.theme.DocuFindWhite
import com.docufind.app.ui.util.docuFindSafeArea

@Composable
fun VaultUnlockFlow(
    authError: String?,
    isAuthenticating: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onDismiss: () -> Unit,
    onPinComplete: (CharArray) -> Unit,
    onBiometric: () -> Unit,
    onForgotPinBiometricReset: () -> Unit,
    onForgotPinConfirmReset: () -> Unit,
    modifier: Modifier = Modifier,
    autoPromptBiometric: Boolean = false
) {
    var showPinEntry by remember { mutableStateOf(false) }
    var pinDigits by remember { mutableStateOf("") }
    var errorShakeKey by remember { mutableIntStateOf(0) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var biometricPrompted by remember { mutableStateOf(false) }

    LaunchedEffect(authError) {
        if (!authError.isNullOrBlank()) {
            pinDigits = ""
            errorShakeKey++
        }
    }

    LaunchedEffect(pinDigits, showPinEntry, isAuthenticating) {
        if (showPinEntry && !isAuthenticating && pinDigits.length == 6) {
            onPinComplete(pinDigits.toCharArray())
            pinDigits = ""
        }
    }

    LaunchedEffect(autoPromptBiometric, showPinEntry, biometricEnabled, biometricAvailable) {
        if (autoPromptBiometric && !showPinEntry && biometricEnabled && biometricAvailable && !biometricPrompted) {
            biometricPrompted = true
            onBiometric()
        }
    }

    if (showForgotDialog) {
        PinForgotDialog(
            biometricEnabled = biometricEnabled && biometricAvailable,
            onDismiss = { showForgotDialog = false },
            onResetWithBiometric = {
                showForgotDialog = false
                onForgotPinBiometricReset()
            },
            onConfirmManualReset = {
                showForgotDialog = false
                onForgotPinConfirmReset()
            }
        )
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(DocuFindVaultDeep, DocuFindVaultBackground, DocuFindVaultDeep)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .docuFindSafeArea()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showPinEntry) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.unlock),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DocuFindWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.vault_unlock_enter_pin),
                style = MaterialTheme.typography.bodyMedium,
                color = DocuFindWhite.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            PinDotIndicator(
                filledCount = pinDigits.length,
                lightMode = true,
                errorShakeKey = errorShakeKey
            )
            if (!authError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PinKeypad(
                onDigit = { digit ->
                    if (pinDigits.length < 6) pinDigits += digit.toString()
                },
                onDelete = {
                    if (pinDigits.isNotEmpty()) pinDigits = pinDigits.dropLast(1)
                },
                lightKeys = true,
                enabled = !isAuthenticating
            )
            TextButton(onClick = { showForgotDialog = true }) {
                Text(stringResource(R.string.forgot_pin), color = DocuFindWhite.copy(alpha = 0.85f))
            }
            TextButton(
                onClick = {
                    showPinEntry = false
                    pinDigits = ""
                },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Text(stringResource(R.string.use_pin_instead_back), color = DocuFindWhite.copy(alpha = 0.9f))
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VaultShieldBadge()
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.unlock),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DocuFindWhite.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.vault_locked_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DocuFindWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.vault_locked_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DocuFindWhite.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center
                )
                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = authError,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                DocuFindPrimaryButton(
                    text = stringResource(R.string.unlock),
                    onClick = { showPinEntry = true },
                    enabled = !isAuthenticating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (biometricEnabled && biometricAvailable) {
                    TextButton(onClick = onBiometric, enabled = !isAuthenticating) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = DocuFindWhite)
                        Text(
                            text = stringResource(R.string.use_fingerprint),
                            color = DocuFindWhite,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else if (biometricAvailable) {
                    TextButton(onClick = { showPinEntry = true }, enabled = !isAuthenticating) {
                        Text(
                            text = stringResource(R.string.use_pin_instead),
                            color = DocuFindWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = DocuFindWhite.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
fun VaultShieldBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(DocuFindVaultGlow.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(DocuFindBlue.copy(alpha = 0.35f))
        )
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = DocuFindWhite.copy(alpha = 0.95f)
        )
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = DocuFindVaultGlow
        )
    }
}

@Composable
private fun PinForgotDialog(
    biometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onResetWithBiometric: () -> Unit,
    onConfirmManualReset: () -> Unit
) {
    var showConfirmReset by remember { mutableStateOf(false) }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.forgot_pin_reset_title)) },
            text = { Text(stringResource(R.string.forgot_pin_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = onConfirmManualReset) {
                    Text(stringResource(R.string.forgot_pin_reset_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.forgot_pin)) },
        text = {
            Text(
                if (biometricEnabled) {
                    stringResource(R.string.forgot_pin_biometric_hint)
                } else {
                    stringResource(R.string.forgot_pin_no_biometric)
                }
            )
        },
        confirmButton = {
            if (biometricEnabled) {
                TextButton(onClick = onResetWithBiometric) {
                    Text(stringResource(R.string.forgot_pin_use_biometric))
                }
            } else {
                TextButton(onClick = { showConfirmReset = true }) {
                    Text(stringResource(R.string.forgot_pin_reset_action))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
