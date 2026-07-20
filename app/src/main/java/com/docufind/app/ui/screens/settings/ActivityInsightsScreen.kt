package com.docufind.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.domain.model.ActivityInsights
import com.docufind.app.domain.model.InsightCategoryCount
import com.docufind.app.domain.model.InsightPeriod
import com.docufind.app.domain.model.InsightTrendPoint
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.screens.add.AttachmentHelper
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityInsightsScreen(
    onBack: () -> Unit,
    viewModel: ActivityInsightsViewModel = hiltViewModel()
) {
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.period.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Insights", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyCard()
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onSelect = viewModel::selectPeriod
            )
            SummaryGrid(insights)
            TrendCard("Activity", "App opens, screen visits, searches and vault opens", insights.activityTrend)
            TrendCard("Documents added", "Records saved in this period", insights.documentsAddedTrend)
            TrendCard("Files stored", "Attachments added to your private vault", insights.filesStoredTrend)
            CategoryCountCard(insights.categoryCounts)
            ReminderSummaryCard(insights)
        }
    }
}

@Composable
private fun PrivacyCard() {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Private by design",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "All activity insights stay on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PeriodSelector(
    selectedPeriod: InsightPeriod,
    onSelect: (InsightPeriod) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightPeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelect(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SummaryGrid(insights: ActivityInsights) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip("Opened", insights.appOpenedCount.toString(), Icons.Default.Analytics)
        SummaryChip("Time", formatDuration(insights.sessionDurationMs), Icons.Default.Timer)
        SummaryChip("Files", insights.filesStoredCount.toString(), Icons.Default.Folder)
        SummaryChip("Storage", AttachmentHelper.formatFileSize(insights.storageUsageBytes), Icons.Default.Folder)
        SummaryChip("Vault opens", insights.vaultOpenCount.toString(), Icons.Default.Lock)
        SummaryChip("Searches", insights.searchUsageCount.toString(), Icons.Default.Search)
        SummaryChip("Screens", insights.screensVisitedCount.toString(), Icons.Default.Analytics)
    }
}

@Composable
private fun SummaryChip(label: String, value: String, icon: ImageVector) {
    AssistChip(
        onClick = {},
        label = {
            Column(modifier = Modifier.widthIn(min = 76.dp, max = 132.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = {
            Icon(icon, contentDescription = null)
        }
    )
}

@Composable
private fun TrendCard(
    title: String,
    subtitle: String,
    points: List<InsightTrendPoint>
) {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MiniBarChart(points)
        }
    }
}

@Composable
private fun MiniBarChart(points: List<InsightTrendPoint>) {
    val maxValue = max(1, points.maxOfOrNull { it.value } ?: 0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        points.forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 40.dp, max = 56.dp),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(point.value / maxValue.toFloat())
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    text = point.value.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(min = 24.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryCountCard(categories: List<InsightCategoryCount>) {
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Category-wise documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (categories.isEmpty()) {
                Text(
                    text = "No documents stored yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MiniBarChart(categories.map { InsightTrendPoint(it.label, it.count) })
            }
        }
    }
}

@Composable
private fun ReminderSummaryCard(insights: ActivityInsights) {
    val summary = insights.reminderSummary
    DocuFindCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Reminder completion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            MiniBarChart(
                listOf(
                    InsightTrendPoint("Created", summary.total),
                    InsightTrendPoint("Done", summary.completed),
                    InsightTrendPoint("Active", summary.active),
                    InsightTrendPoint("Off", summary.disabled)
                )
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = durationMs / 60_000
    if (minutes < 1) return "<1m"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) "${hours}h ${remainingMinutes}m" else "${minutes}m"
}
