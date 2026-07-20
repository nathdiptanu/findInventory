package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.theme.DocuFindBlueLight
import com.docufind.app.ui.theme.DocuFindTeal

private data class HowToUseStep(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val accentTeal: Boolean = false
)

private val howToUseSteps = listOf(
    HowToUseStep(Icons.Default.Add, R.string.how_to_use_add_title, R.string.how_to_use_add_desc),
    HowToUseStep(Icons.Default.Folder, R.string.how_to_use_organize_title, R.string.how_to_use_organize_desc),
    HowToUseStep(Icons.Default.Shield, R.string.how_to_use_secure_title, R.string.how_to_use_secure_desc, accentTeal = true),
    HowToUseStep(Icons.Default.Notifications, R.string.how_to_use_remind_title, R.string.how_to_use_remind_desc),
    HowToUseStep(Icons.Default.Search, R.string.how_to_use_find_title, R.string.how_to_use_find_desc),
    HowToUseStep(Icons.Default.Lock, R.string.how_to_use_access_title, R.string.how_to_use_access_desc)
)

@Composable
fun HowToUseContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        howToUseSteps.forEachIndexed { index, step ->
            HowToUseStepRow(step = step)
            if (index < howToUseSteps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun HowToUseStepRow(step: HowToUseStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DocuFindBlueLight.copy(alpha = 0.55f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = if (step.accentTeal) DocuFindTeal else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(step.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
