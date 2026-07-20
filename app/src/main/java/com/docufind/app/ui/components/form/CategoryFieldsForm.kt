package com.docufind.app.ui.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.domain.model.CategoryFieldDef
import com.docufind.app.domain.model.CategoryFieldKind
import com.docufind.app.domain.model.CategoryFieldRegistry
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.DocuFindDateField
import com.docufind.app.ui.util.FormValidation

@Composable
fun CategoryFieldsForm(
    categoryId: String,
    values: Map<String, String>,
    onValueChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fields = CategoryFieldRegistry.fieldsFor(categoryId)
    if (fields.isEmpty()) return

    DocuFindCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.category_details_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = categoryHint(categoryId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CategoryFieldRegistry.discretionWarningKey(categoryId)?.let { key ->
                val resId = when (key) {
                    "cards_sensitive_warning" -> R.string.cards_sensitive_warning
                    "banking_discretion_warning" -> R.string.banking_discretion_warning
                    else -> null
                }
                resId?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            fields.forEach { field ->
                CategoryFieldInput(
                    field = field,
                    value = values[field.key].orEmpty(),
                    onValueChange = { onValueChange(field.key, it) }
                )
            }
        }
    }
}

@Composable
private fun categoryHint(categoryId: String): String = stringResource(
    when (categoryId) {
        "documents" -> R.string.category_hint_documents
        "id_cards" -> R.string.category_hint_id_cards
        "cards" -> R.string.category_hint_cards
        "finance" -> R.string.category_hint_finance
        else -> R.string.category_hint_default
    }
)

@Composable
private fun CategoryFieldInput(
    field: CategoryFieldDef,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (field.kind) {
        CategoryFieldKind.DATE -> {
            DocuFindDateField(
                label = field.label,
                epochMillis = value.toLongOrNull(),
                onDateSelected = { selected ->
                    onValueChange(selected?.toString().orEmpty())
                },
                allowClear = true
            )
        }
        CategoryFieldKind.CHOICE -> {
            DocuFindOptionPicker(
                label = field.label,
                options = field.choices,
                selected = value,
                onSelected = onValueChange
            )
        }
        CategoryFieldKind.MULTILINE -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        CategoryFieldKind.PASSWORD -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }
        CategoryFieldKind.SENSITIVE, CategoryFieldKind.TEXT -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !field.multiline,
                isError = when (field.key) {
                    "ifsc" -> FormValidation.ifscError(value)
                    "micr" -> FormValidation.micrError(value)
                    else -> false
                },
                supportingText = {
                    when {
                        field.key == "ifsc" && FormValidation.ifscError(value) ->
                            Text("Enter a valid 11-character IFSC")
                        field.key == "micr" && FormValidation.micrError(value) ->
                            Text("Enter a valid 9-digit MICR")
                    }
                }
            )
        }
    }
}
