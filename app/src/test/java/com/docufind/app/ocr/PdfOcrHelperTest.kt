package com.docufind.app.ocr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfOcrHelperTest {

    @Test
    fun recognizePdf_returnsCorruptOrEngineErrorForInvalidFile() = runTest {
        val helper = PdfOcrHelper { "" }
        val invalid = File.createTempFile("invalid", ".pdf")
        invalid.writeText("not a pdf")

        val result = helper.recognizePdf(invalid) {}

        assertTrue(result is OcrResult.Failure)
        val reason = (result as OcrResult.Failure).reason
        assertTrue(
            reason == OcrFailureReason.CORRUPT_FILE ||
                reason == OcrFailureReason.ENGINE_ERROR ||
                reason == OcrFailureReason.PASSWORD_PROTECTED_PDF
        )
        invalid.delete()
    }

    @Test
    fun combinePageText_joinsNonBlankParts() {
        val combined = PdfOcrHelper.combinePageText(listOf("Line one", "", "Line two"))
        assertEquals("Line one\n\nLine two", combined)
    }

    @Test
    fun combinePageText_returnsEmptyWhenNoText() {
        assertEquals("", PdfOcrHelper.combinePageText(emptyList()))
    }
}
