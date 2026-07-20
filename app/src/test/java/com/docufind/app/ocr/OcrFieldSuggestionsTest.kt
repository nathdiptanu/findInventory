package com.docufind.app.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OcrFieldSuggestionsTest {
    @Test
    fun extract_findsIfscAndDates() {
        val text = """
            Bank of Example
            IFSC: HDFC0001234
            Issue 01/01/2024
            Valid till 31/12/2026
        """.trimIndent()
        val suggestion = OcrFieldSuggestions.extract(text, "finance")
        assertThat(suggestion.fieldValues["ifsc"]).isEqualTo("HDFC0001234")
        assertThat(suggestion.issueDateMillis).isNotNull()
        assertThat(suggestion.expiryDateMillis).isNotNull()
    }

    @Test
    fun extract_cardsLastFour() {
        val suggestion = OcrFieldSuggestions.extract("Card ending in 4321", "cards")
        assertThat(suggestion.fieldValues["last_four"]).isEqualTo("4321")
    }
}
