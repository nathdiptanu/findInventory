package com.docufind.app.ui.screens.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.components.VaultUnlockFlow
import com.docufind.app.ui.screens.setup.SecuritySetupFlow
import com.docufind.app.ui.theme.DocuFindVaultBackground
import com.docufind.app.ui.util.rememberFragmentActivity
import kotlinx.coroutines.delay

@Composable
fun VaultScreen(viewModel: VaultViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val activity = rememberFragmentActivity()

    ForceSecureScreenEffect()

    Scaffold(containerColor = DocuFindVaultBackground) { padding ->
        VaultUnlockFlow(
            modifier = Modifier.fillMaxSize().padding(padding),
            authError = authError,
            isAuthenticating = isAuthenticating,
            biometricEnabled = uiState.biometricEnabled,
            biometricAvailable = uiState.biometricAvailable,
            onDismiss = { /* vault tab has no dismiss */ },
            onPinComplete = { pin ->
                activity?.let { viewModel.unlockWithPin(it, pin) }
            },
            onBiometric = {
                activity?.let { viewModel.unlockWithBiometric(it) }
            },
            onForgotPinBiometricReset = {
                activity?.let { viewModel.resetPinWithBiometric(it) }
            },
            onForgotPinConfirmReset = { viewModel.resetPinWithoutBiometric() },
            autoPromptBiometric = false
        )
    }
}

@Composable
fun VaultTabScreen(
    onRecordClick: (String) -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOpeningAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.pinConfigured, uiState.isLocked) {
        if (uiState.pinConfigured && !uiState.isLocked) {
            showOpeningAnimation = true
            delay(2_300)
            showOpeningAnimation = false
        }
    }

    when {
        !uiState.pinConfigured -> {
            SecuritySetupFlow(
                onComplete = { viewModel.onSecuritySetupComplete() }
            )
        }
        uiState.isLocked -> {
            VaultScreen(viewModel = viewModel)
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                com.docufind.app.ui.screens.search.SearchScreen(onRecordClick = onRecordClick)
                AnimatedVisibility(visible = showOpeningAnimation) {
                    VaultOpeningAnimation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun VaultOpeningAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vault-opening")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault-pulse"
    )
    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault-glow"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF1265E1).copy(alpha = 0.42f), DocuFindVaultBackground),
                radius = 900f
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .scale(pulse)
                    .background(Color(0xFF1265E1).copy(alpha = 0.16f * glow), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(92.dp),
                    color = Color(0xFF67C3FF),
                    strokeWidth = 3.dp
                )
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF67C3FF),
                    modifier = Modifier.size(58.dp)
                )
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .alpha(0.94f)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Opening your secure vault...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Preparing your protected documents...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8F2FF)
                )
            }
        }
    }
}
