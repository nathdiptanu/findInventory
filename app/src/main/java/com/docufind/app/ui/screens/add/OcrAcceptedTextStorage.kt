package com.docufind.app.ui.screens.add

import com.docufind.app.security.SearchIndexPolicy

object OcrAcceptedTextStorage {
    data class Applied(val notes: String, val tagsText: String)

    fun apply(existingNotes: String, existingTags: String, acceptedText: String): Applied {
        val trimmed = acceptedText.trim()
        if (trimmed.isEmpty()) {
            return Applied(existingNotes, existingTags)
        }
        return if (SearchIndexPolicy.allowsOcrTextInSearchIndex()) {
            Applied(
                notes = existingNotes,
                tagsText = mergeDelimitedText(existingTags, trimmed)
            )
        } else {
            Applied(
                notes = mergeBlockText(existingNotes, trimmed),
                tagsText = existingTags
            )
        }
    }

    private fun mergeBlockText(existing: String, addition: String): String =
        if (existing.isBlank()) addition else "$existing\n\n$addition"

    private fun mergeDelimitedText(existing: String, addition: String): String =
        if (existing.isBlank()) addition else "$existing, $addition"
}
