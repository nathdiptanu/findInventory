package com.docufind.app.unit.domain

import com.docufind.app.domain.model.CategoryFieldKind
import com.docufind.app.domain.model.CategoryFieldRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class CategoryFieldRegistryTest {

    @Test
    fun bankingFields_includeSensitiveKinds() {
        val fields = CategoryFieldRegistry.fieldsFor("finance")
        assertThat(fields.map { it.kind }).contains(CategoryFieldKind.SENSITIVE)
        assertThat(fields.map { it.kind }).contains(CategoryFieldKind.PASSWORD)
        assertThat(fields.map { it.key }).contains("account_number")
        assertThat(fields.map { it.key }).contains("ifsc")
    }

    @Test
    fun propertyFields_areComplete() {
        val fields = CategoryFieldRegistry.fieldsFor("property")
        assertThat(fields).isNotEmpty()
        assertThat(fields.map { it.key }).containsAtLeast(
            "property_type",
            "ownership",
            "address",
            "tax_due_date",
            "maintenance_due_date"
        )
    }

    @Test
    fun cardsFields_haveDiscretionWarning() {
        assertThat(CategoryFieldRegistry.discretionWarningKey("cards")).isEqualTo("cards_sensitive_warning")
    }

    @Test
    fun finance_usesOpeningAndMaturityDates() {
        assertThat(CategoryFieldRegistry.usesIssueDate("finance")).isTrue()
        assertThat(CategoryFieldRegistry.usesExpiryDate("finance")).isTrue()
        assertThat(CategoryFieldRegistry.issueDateLabel("finance")).isEqualTo("Opening date")
        assertThat(CategoryFieldRegistry.expiryDateLabel("finance")).isEqualTo("Maturity date")
        assertThat(CategoryFieldRegistry.showReminderByDefault("finance")).isTrue()
    }

    @Test
    fun property_usesPurchaseAndRenewalDates() {
        assertThat(CategoryFieldRegistry.usesIssueDate("property")).isTrue()
        assertThat(CategoryFieldRegistry.usesExpiryDate("property")).isTrue()
        assertThat(CategoryFieldRegistry.issueDateLabel("property")).isEqualTo("Purchase date")
        assertThat(CategoryFieldRegistry.expiryDateLabel("property")).isEqualTo("Agreement renewal date")
        assertThat(CategoryFieldRegistry.showReminderByDefault("property")).isTrue()
    }

    @Test
    fun insurance_showsReminderByDefault() {
        assertThat(CategoryFieldRegistry.showReminderByDefault("insurance")).isTrue()
    }

    @Test
    fun requiredVaultCategories_haveDynamicFieldCoverage() {
        val requiredCategories = listOf(
            "documents",
            "id_cards",
            "cards",
            "medical",
            "prescriptions",
            "vaccination",
            "education",
            "finance",
            "property",
            "insurance",
            "vehicle",
            "warranty",
            "pets",
            "family",
            "emergency",
            "others"
        )

        requiredCategories.forEach { category ->
            assertWithMessage("fields for $category")
                .that(CategoryFieldRegistry.fieldsFor(category))
                .isNotEmpty()
        }
    }

    @Test
    fun dateHeavyCategories_usePrimaryDatePickerFields() {
        assertThat(CategoryFieldRegistry.usesIssueDate("documents")).isTrue()
        assertThat(CategoryFieldRegistry.usesExpiryDate("documents")).isTrue()
        assertThat(CategoryFieldRegistry.usesExpiryDate("cards")).isTrue()
        assertThat(CategoryFieldRegistry.usesExpiryDate("warranty")).isTrue()
        assertThat(CategoryFieldRegistry.expiryDateLabel("warranty")).isEqualTo("Warranty expiry")
        assertThat(CategoryFieldRegistry.expiryDateLabel("insurance")).isEqualTo("Renewal / expiry date")
    }

    @Test
    fun typeChoiceFields_areNotDuplicatedBesideSubcategory() {
        val duplicateKeys = setOf(
            "document_type",
            "id_type",
            "card_type",
            "insurance_type",
            "pet_record_type",
            "record_type"
        )
        listOf("documents", "id_cards", "cards", "insurance", "pets", "medical", "education").forEach { category ->
            val keys = CategoryFieldRegistry.fieldsFor(category).map { it.key }
            assertWithMessage("category $category should not repeat type dropdowns")
                .that(keys.intersect(duplicateKeys))
                .isEmpty()
        }
    }

    @Test
    fun unknownCategory_returnsNoFields() {
        assertThat(CategoryFieldRegistry.fieldsFor("unknown")).isEmpty()
    }
}
