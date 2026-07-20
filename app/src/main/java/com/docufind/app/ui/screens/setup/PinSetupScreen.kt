package com.docufind.app.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.security.protection.ForceSecureScreenEffect
import com.docufind.app.ui.components.PinDotIndicator
import com.docufind.app.ui.components.PinKeypad
import com.docufind.app.ui.theme.DocuFindBlue
import com.docufind.app.ui.theme.DocuFindBlueLight
import com.docufind.app.ui.theme.DocuFindNavy
import com.docufind.app.ui.util.docuFindSafeArea

@Composable
fun PinSetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.pinUiState.collectAsStateWithLifecycle()

    ForceSecureScreenEffect()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DocuFindBlueLight.copy(alpha = 0.35f), MaterialTheme.colorScheme.background)
                    )
                )
                .docuFindSafeArea()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Surface(
                shape = CircleShape,
                color = DocuFindBlue.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DocuFindNavy,
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (uiState.isConfirmStep) {
                    stringResource(R.string.confirm_pin)
                } else {
                    stringResource(R.string.create_pin)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!uiState.isConfirmStep) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_pin_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            PinDotIndicator(filledCount = uiState.enteredLength)
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                PinKeypad(
                    onDigit = viewModel::onPinDigit,
                    onDelete = viewModel::onPinDelete,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                )
            }
        }
    }

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) {
            onComplete()
            viewModel.resetPinCompleted()
        }
    }
}
