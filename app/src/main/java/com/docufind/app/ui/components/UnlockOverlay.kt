package com.docufind.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.theme.DocuFindVaultDeep

@Composable
fun UnlockOverlay(
    visible: Boolean,
    authError: String?,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    isAuthenticating: Boolean,
    onDismiss: () -> Unit,
    onPinComplete: (CharArray) -> Unit,
    onBiometric: () -> Unit,
    onForgotPinBiometricReset: () -> Unit,
    onForgotPinConfirmReset: () -> Unit
) {
    if (!visible) return

    ForceSecureScreenEffect()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DocuFindVaultDeep) {
            VaultUnlockFlow(
                authError = authError,
                isAuthenticating = isAuthenticating,
                biometricEnabled = biometricEnabled,
                biometricAvailable = biometricAvailable,
                onDismiss = onDismiss,
                onPinComplete = onPinComplete,
                onBiometric = onBiometric,
                onForgotPinBiometricReset = onForgotPinBiometricReset,
                onForgotPinConfirmReset = onForgotPinConfirmReset,
                autoPromptBiometric = true
            )
        }
    }
}
