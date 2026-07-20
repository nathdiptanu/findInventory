package com.docufind.app.ui.screens.placeholder

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.docufind.app.R
import com.docufind.app.domain.model.QuickAccessItem

@Composable
fun CategoryPlaceholderScreen(
    categoryId: String,
    onBack: () -> Unit
) {
    val title = QuickAccessItem.entries.find { it.id == categoryId }?.displayName
        ?: categoryId.replace('_', ' ').replaceFirstChar { it.uppercase() }

    SectionPlaceholderScreen(
        title = title,
        onBack = onBack,
        description = stringResource(R.string.category_placeholder_desc, title)
    )
}
