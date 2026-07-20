package com.docufind.app.ui.screens.add

import com.docufind.app.security.SearchIndexPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrAcceptedTextStorageTest {

    @Test
    fun apply_storesInNotesWhenSearchIndexPolicyDisallowsOcr() {
        val result = OcrAcceptedTextStorage.apply(
            existingNotes = "Existing note",
            existingTags = "tag1",
            acceptedText = "Extracted line"
        )
        assertEquals("Existing note\n\nExtracted line", result.notes)
        assertEquals("tag1", result.tagsText)
    }

    @Test
    fun apply_usesNotesOnlyBecausePolicyDisallowsSearchIndex() {
        assertEquals(false, SearchIndexPolicy.allowsOcrTextInSearchIndex())
        val result = OcrAcceptedTextStorage.apply(
            existingNotes = "",
            existingTags = "",
            acceptedText = "Only notes"
        )
        assertEquals("Only notes", result.notes)
        assertEquals("", result.tagsText)
    }
}
