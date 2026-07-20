package com.docufind.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.QuickAccessSummary

@Composable
fun QuickAccessGrid(
    items: List<QuickAccessSummary>,
    onItemClick: (QuickAccessItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { summary ->
                    QuickAccessTile(
                        summary = summary,
                        onClick = { onItemClick(summary.item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    summary: QuickAccessSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = quickAccessAccent(summary.item)
    DocuFindCard(
        modifier = modifier.defaultMinSize(minHeight = 96.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = quickAccessIcon(summary.item),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = summary.item.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 2.dp, end = 2.dp)
                )
                if (summary.item != QuickAccessItem.MORE && summary.itemCount > 0) {
                    Text(
                        text = "${summary.itemCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun quickAccessAccent(item: QuickAccessItem): Color = when (item) {
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
    QuickAccessItem.PETS -> Color(0xFFBF360C)
    QuickAccessItem.FAMILY -> Color(0xFF5E35B1)
    QuickAccessItem.EMERGENCY -> Color(0xFFD32F2F)
    QuickAccessItem.REMINDERS -> Color(0xFFF4511E)
    QuickAccessItem.BANKING -> Color(0xFF1565C0)
    QuickAccessItem.PROPERTY -> Color(0xFF6D4C41)
    QuickAccessItem.MORE -> Color(0xFF78909C)
}

private fun quickAccessIcon(item: QuickAccessItem): ImageVector = when (item) {
    QuickAccessItem.DOCUMENTS -> Icons.Default.Description
    QuickAccessItem.ID_CARDS -> Icons.Default.Badge
    QuickAccessItem.CARDS -> Icons.Default.CreditCard
    QuickAccessItem.MEDICAL -> Icons.Default.MedicalServices
    QuickAccessItem.PRESCRIPTIONS -> Icons.Default.LocalPharmacy
    QuickAccessItem.VACCINATION -> Icons.Default.Vaccines
    QuickAccessItem.EDUCATION -> Icons.Default.School
    QuickAccessItem.INSURANCE -> Icons.Default.Shield
    QuickAccessItem.VEHICLE -> Icons.Default.DirectionsCar
    QuickAccessItem.WARRANTY -> Icons.Default.VerifiedUser
    QuickAccessItem.PETS -> Icons.Default.Pets
    QuickAccessItem.FAMILY -> Icons.Default.FamilyRestroom
    QuickAccessItem.EMERGENCY -> Icons.Default.ContactEmergency
    QuickAccessItem.REMINDERS -> Icons.Default.Notifications
    QuickAccessItem.BANKING -> Icons.Default.AccountBalance
    QuickAccessItem.PROPERTY -> Icons.Default.Home
    QuickAccessItem.MORE -> Icons.Default.MoreHoriz
}
