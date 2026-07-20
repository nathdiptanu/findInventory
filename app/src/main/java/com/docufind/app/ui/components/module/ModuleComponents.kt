package com.docufind.app.ui.components.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.components.DocuFindEmptyState

@Composable
fun ModuleFilterChips(
    chips: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            FilterChip(
                selected = selected == chip,
                onClick = { onSelected(chip) },
                label = { Text(chip) }
            )
        }
    }
}

@Composable
fun ModuleEmptyState(
    categoryId: String,
    moduleTitle: String,
    modifier: Modifier = Modifier
) {
    val message = when (categoryId) {
        "documents" -> stringResource(R.string.documents_empty)
        else -> stringResource(R.string.module_empty_message, moduleTitle)
    }
    DocuFindEmptyState(message = message, modifier = modifier)
}
