package com.docufind.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.theme.DocuFindBlueLight
import com.docufind.app.ui.theme.DocuFindTeal

@Composable
fun TagItTwoWaysSection(modifier: Modifier = Modifier) {
    DocuFindCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.tag_it_two_ways_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.tag_it_two_ways_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TagHelperTile(
                    icon = Icons.Default.Folder,
                    label = stringResource(R.string.tag_helper_category),
                    modifier = Modifier.weight(1f)
                )
                TagHelperTile(
                    icon = Icons.Default.People,
                    label = stringResource(R.string.tag_helper_family),
                    modifier = Modifier.weight(1f),
                    accentTeal = true
                )
                TagHelperTile(
                    icon = Icons.Default.Pets,
                    label = stringResource(R.string.tag_helper_pet),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TagHelperTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    accentTeal: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DocuFindBlueLight.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accentTeal) DocuFindTeal else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
