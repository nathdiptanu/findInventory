package com.docufind.app.ui.screens.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel

private enum class SecuritySetupStep {
    PIN,
    BIOMETRIC
}

/**
 * Deferred PIN setup followed by optional biometric enrollment.
 * Used when the user first opens Vault or saves their first secure document.
 */
@Composable
fun SecuritySetupFlow(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(SecuritySetupStep.PIN) }
    val biometricAvailable = remember { viewModel.isBiometricAvailable() }

    when (step) {
        SecuritySetupStep.PIN -> {
            PinSetupScreen(
                onComplete = {
                    if (biometricAvailable) {
                        step = SecuritySetupStep.BIOMETRIC
                    } else {
                        viewModel.finishSecuritySetup(unlockVault = true, onDone = onComplete)
                    }
                },
                viewModel = viewModel
            )
        }
        SecuritySetupStep.BIOMETRIC -> {
            BiometricSetupScreen(
                onComplete = {
                    viewModel.finishSecuritySetup(unlockVault = true, onDone = onComplete)
                },
                viewModel = viewModel
            )
        }
    }
}
