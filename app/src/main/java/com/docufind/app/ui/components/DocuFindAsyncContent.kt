package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R

@Composable
fun DocuFindErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    DocuFindCard(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DocuFindEmptyState(message = message, title = stringResource(R.string.screen_error_title))
            onRetry?.let { retry ->
                TextButton(
                    onClick = retry,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
fun DocuFindAsyncContent(
    isLoading: Boolean,
    errorMessage: String? = null,
    isEmpty: Boolean = false,
    emptyMessage: String = "",
    emptyTitle: String? = null,
    loadingMessage: String = stringResource(R.string.loading),
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                DocuFindLoadingState(message = loadingMessage)
            }
            errorMessage != null -> {
                DocuFindErrorState(
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.padding(16.dp)
                )
            }
            isEmpty -> {
                DocuFindEmptyState(
                    message = emptyMessage,
                    title = emptyTitle,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> content()
        }
    }
}
