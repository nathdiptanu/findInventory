package com.docufind.app.export.pdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PdfExportFilenameSanitizerTest {

    @Test
    fun sanitize_stripsUnsafeCharacters() {
        val result = PdfExportFilenameSanitizer.sanitize("My/PAN Card: 2024")
        assertThat(result).doesNotContain("/")
        assertThat(result).doesNotContain(":")
        assertThat(result).endsWith("_DocuFind.pdf")
    }

    @Test
    fun sanitize_preservesDevanagariNames() {
        val result = PdfExportFilenameSanitizer.sanitize("राजेश कुमार")
        assertThat(result).contains("राज")
        assertThat(result).endsWith("_DocuFind.pdf")
    }

    @Test
    fun uniqueName_appendsTimestamp() {
        val first = PdfExportFilenameSanitizer.uniqueName("Passport")
        val second = PdfExportFilenameSanitizer.uniqueName("Passport")
        assertThat(first).isNotEqualTo(second)
        assertThat(first).contains("Passport")
    }
}
