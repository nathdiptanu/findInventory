package com.docufind.app.ocr

import com.docufind.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrResultMapperTest {

    @Test
    fun messageResId_mapsEmptyResult() {
        assertEquals(R.string.ocr_empty, OcrResultMapper.messageResId(OcrResult.Empty))
    }

    @Test
    fun messageResId_returnsNullForSuccess() {
        assertNull(OcrResultMapper.messageResId(OcrResult.Success("hello")))
    }

    @Test
    fun failureMessageResId_mapsPasswordProtectedPdf() {
        assertEquals(
            R.string.ocr_password_protected_pdf,
            OcrResultMapper.failureMessageResId(OcrFailureReason.PASSWORD_PROTECTED_PDF)
        )
    }

    @Test
    fun failureMessageResId_mapsUnsupportedMime() {
        assertEquals(
            R.string.ocr_unsupported_mime,
            OcrResultMapper.failureMessageResId(OcrFailureReason.UNSUPPORTED_MIME)
        )
    }

    @Test
    fun progressMessageResId_mapsProcessingPage() {
        assertEquals(
            R.string.ocr_progress_page,
            OcrResultMapper.progressMessageResId(OcrProgress.ProcessingPage(2, 5))
        )
    }

    @Test
    fun progressMessageArgs_includesPageNumbers() {
        assertEquals(
            arrayOf<Any>(2, 5),
            OcrResultMapper.progressMessageArgs(OcrProgress.ProcessingPage(2, 5))
        )
    }
}
