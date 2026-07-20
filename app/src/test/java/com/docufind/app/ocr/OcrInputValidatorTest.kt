package com.docufind.app.ocr

import com.docufind.app.R
import com.docufind.app.security.file.MAX_VAULT_FILE_BYTES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrInputValidatorTest {

    @Test
    fun validate_acceptsSupportedImageMimeWithinSizeLimit() {
        val result = OcrInputValidator.validate("image/jpeg", 1024L)
        assertTrue(result is OcrValidationResult.Valid)
        assertEquals("image/jpeg", (result as OcrValidationResult.Valid).mimeType)
    }

    @Test
    fun validate_acceptsPdfWithinSizeLimit() {
        val result = OcrInputValidator.validate("application/pdf", MAX_VAULT_FILE_BYTES)
        assertTrue(result is OcrValidationResult.Valid)
    }

    @Test
    fun validate_rejectsUnsupportedMime() {
        val result = OcrInputValidator.validate("text/plain", 1024L)
        assertEquals(OcrValidationResult.UnsupportedMime, result)
        assertEquals(
            OcrFailureReason.UNSUPPORTED_MIME,
            OcrInputValidator.validationFailureReason(result)
        )
    }

    @Test
    fun validate_rejectsNullMime() {
        val result = OcrInputValidator.validate(null, 1024L)
        assertEquals(OcrValidationResult.UnsupportedMime, result)
    }

    @Test
    fun validate_rejectsOversizedFile() {
        val result = OcrInputValidator.validate("image/png", MAX_VAULT_FILE_BYTES + 1)
        assertEquals(OcrValidationResult.FileTooLarge, result)
        assertEquals(
            OcrFailureReason.FILE_TOO_LARGE,
            OcrInputValidator.validationFailureReason(result)
        )
    }

    @Test
    fun validate_rejectsUnknownSize() {
        val result = OcrInputValidator.validate("image/png", 0L)
        assertEquals(OcrValidationResult.UnknownSize, result)
        assertEquals(
            OcrFailureReason.CORRUPT_FILE,
            OcrInputValidator.validationFailureReason(result)
        )
    }

    @Test
    fun isOcrEligible_trueForSupportedTypes() {
        assertTrue(OcrInputValidator.isOcrEligible("image/jpeg"))
        assertTrue(OcrInputValidator.isOcrEligible("application/pdf"))
    }
}
