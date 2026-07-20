package com.docufind.app.ocr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MlKitOcrEngineTest {

    @Test
    fun recognize_returnsUnsupportedMimeForUnknownType() = runTest {
        val engine = MlKitOcrEngine(FakeTextRecognizerProvider(), kotlinx.coroutines.Dispatchers.Unconfined)
        val tempFile = File.createTempFile("ocr_test", ".txt")

        val result = engine.recognize(tempFile, "text/plain")

        assertTrue(result is OcrResult.Failure)
        assertEquals(
            OcrFailureReason.UNSUPPORTED_MIME,
            (result as OcrResult.Failure).reason
        )
        tempFile.delete()
    }

    private class FakeTextRecognizerProvider : TextRecognizerProvider {
        override fun create() = throw UnsupportedOperationException("Not used in this test")
    }
}
