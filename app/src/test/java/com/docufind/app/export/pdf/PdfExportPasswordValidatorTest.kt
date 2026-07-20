package com.docufind.app.export.pdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PdfExportPasswordValidatorTest {

    @Test
    fun validate_rejectsShortPassword() {
        val result = PdfExportPasswordValidator.validate("short".toCharArray())
        assertThat(result).isInstanceOf(PdfExportPasswordValidator.ValidationResult.Invalid::class.java)
    }

    @Test
    fun validate_acceptsStrongPassword() {
        val result = PdfExportPasswordValidator.validate("secure-pass".toCharArray())
        assertThat(result).isEqualTo(PdfExportPasswordValidator.ValidationResult.Valid)
    }

    @Test
    fun passwordsMatch_requiresEqualValues() {
        assertThat(
            PdfExportPasswordValidator.passwordsMatch(
                "same-value".toCharArray(),
                "same-value".toCharArray()
            )
        ).isTrue()
        assertThat(
            PdfExportPasswordValidator.passwordsMatch(
                "first-value".toCharArray(),
                "second-one".toCharArray()
            )
        ).isFalse()
    }
}
