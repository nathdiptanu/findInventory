package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.RecentDocument
import com.docufind.app.domain.model.VaultCategory
import com.docufind.app.ui.components.form.icon

@Composable
fun RecentItemsSection(
    items: List<RecentDocument>,
    onItemClick: (RecentDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recent_items_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (items.isEmpty()) {
            DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.recent_items_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { document ->
                    RecentItemRow(
                        document = document,
                        onClick = { onItemClick(document) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentItemRow(
    document: RecentDocument,
    onClick: () -> Unit
) {
    val categoryLabel = categoryLabelFor(document.categoryId)
    val accent = categoryAccentFor(document.categoryId)
    val icon = categoryIconFor(document.categoryId)

    DocuFindCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recent_items_protected_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = stringResource(R.string.recent_items_protected_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.protected_by_docufind_vault),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun categoryLabelFor(categoryId: String): String {
    VaultCategory.fromId(categoryId)?.displayName?.let { return it }
    QuickAccessItem.entries.find { it.id == categoryId }?.displayName?.let { return it }
    return categoryId.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun categoryIconFor(categoryId: String): ImageVector =
    VaultCategory.fromId(categoryId)?.icon() ?: Icons.Default.Description

private fun categoryAccentFor(categoryId: String): Color {
    val item = QuickAccessItem.entries.find { it.id == categoryId }
    return when (item) {
        QuickAccessItem.DOCUMENTS -> Color(0xFF1E88E5)
        QuickAccessItem.ID_CARDS -> Color(0xFF3949AB)
        QuickAccessItem.CARDS -> Color(0xFF00ACC1)
        QuickAccessItem.MEDICAL -> Color(0xFFE91E63)
        QuickAccessItem.PRESCRIPTIONS -> Color(0xFF8E24AA)
        QuickAccessItem.VACCINATION -> Color(0xFF00897B)
        QuickAccessItem.EDUCATION -> Color(0xFF7E57C2)
        QuickAccessItem.INSURANCE -> Color(0xFF43A047)
        QuickAccessItem.VEHICLE -> Color(0xFFFB8C00)
        QuickAccessItem.WARRANTY -> Color(0xFFFFB300)
        QuickAccessItem.BANKING -> Color(0xFF1565C0)
        QuickAccessItem.PROPERTY -> Color(0xFF6D4C41)
        else -> Color(0xFF78909C)
    }
}

