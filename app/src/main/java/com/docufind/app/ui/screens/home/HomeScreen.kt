package com.docufind.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.docufind.app.R
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.RecentDocument
import com.docufind.app.ui.components.DocuFindLogo
import com.docufind.app.ui.components.DocuFindSearchBar
import com.docufind.app.ui.components.HowToUseSection
import com.docufind.app.ui.components.QuickAccessGrid
import com.docufind.app.ui.components.RecentItemsSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onQuickAccessClick: (QuickAccessItem) -> Unit,
    onRecentItemClick: (RecentDocument) -> Unit = {},
    onExpiringItemClick: (String) -> Unit = {},
    onNavigateReminders: () -> Unit = {},
    onNavigateScanQr: () -> Unit = {},
    onNavigateSearch: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val quickAccess by viewModel.quickAccess.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()
    val expiringSoon by viewModel.expiringSoon.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshGreeting()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refreshHome()
                delay(450)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DocuFindLogo(
                        modifier = Modifier.size(52.dp),
                        size = 52.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.userName.isNotBlank()) {
                                stringResource(R.string.home_welcome, uiState.userName)
                            } else {
                                stringResource(R.string.home_welcome_guest)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (uiState.tagline.isNotBlank()) {
                            Text(
                                text = buildAnnotatedString {
                                    val tagline = uiState.tagline
                                    if (tagline.isNotEmpty()) {
                                        withStyle(
                                            SpanStyle(
                                                color = uiState.taglineAccentColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) {
                                            append(tagline.first())
                                        }
                                        if (tagline.length > 1) {
                                            withStyle(
                                                SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            ) {
                                                append(tagline.substring(1))
                                            }
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onNavigateScanQr) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.qr_scan_content_description),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateReminders) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = stringResource(R.string.nav_reminders),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            DocuFindSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_documents),
                modifier = Modifier.fillMaxWidth(),
                onSearch = { onNavigateSearch(searchQuery) }
            )
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.quick_access_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                QuickAccessGrid(
                    items = quickAccess,
                    onItemClick = onQuickAccessClick
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_expiring_soon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (expiringSoon.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_expiring_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    expiringSoon.forEach { item ->
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExpiringItemClick(item.id) }
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            RecentItemsSection(
                items = recentItems,
                onItemClick = { document ->
                    if (document.id.isNotBlank()) {
                        onRecentItemClick(document)
                    }
                }
            )
        }

        item {
            HowToUseSection(
                expanded = uiState.howToUseExpanded,
                onToggle = viewModel::toggleHowToUseExpanded
            )
        }
    }
    }
}
