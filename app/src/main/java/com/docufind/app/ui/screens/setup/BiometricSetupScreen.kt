package com.docufind.app.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.docufind.app.R
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.ui.components.DocuFindPrimaryButton
import com.docufind.app.ui.theme.DocuFindBlueLight
import com.docufind.app.ui.util.docuFindSafeArea
import com.docufind.app.ui.util.rememberFragmentActivity
import kotlinx.coroutines.launch

@Composable
fun BiometricSetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val activity = rememberFragmentActivity()
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEnrolling by remember { mutableStateOf(false) }
    val biometricAvailable = remember { viewModel.isBiometricAvailable() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .docuFindSafeArea()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = DocuFindBlueLight
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.use_biometrics),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (biometricAvailable) {
                    stringResource(R.string.biometric_setup_subtitle)
                } else {
                    stringResource(R.string.biometric_unavailable)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(48.dp))
            if (biometricAvailable) {
                DocuFindPrimaryButton(
                    text = stringResource(R.string.enable),
                    enabled = !isEnrolling,
                    onClick = {
                        val act = activity ?: return@DocuFindPrimaryButton
                        scope.launch {
                            isEnrolling = true
                            errorMessage = null
                            when (val result = viewModel.enrollBiometric(act)) {
                                is AuthResult.Success -> {
                                    viewModel.enableBiometric()
                                    viewModel.finishSecuritySetup(unlockVault = true, onDone = onComplete)
                                }
                                is AuthResult.Error -> errorMessage = result.message
                                AuthResult.Cancelled -> Unit
                                AuthResult.Failed -> errorMessage = "Biometric authentication failed."
                            }
                            isEnrolling = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(
                onClick = {
                    viewModel.skipBiometric()
                    viewModel.finishSecuritySetup(unlockVault = true, onDone = onComplete)
                },
                enabled = !isEnrolling
            ) {
                Text(
                    if (biometricAvailable) {
                        stringResource(R.string.onboarding_skip)
                    } else {
                        stringResource(R.string.continue_with_pin)
                    }
                )
            }
        }
    }
}
