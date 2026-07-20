package com.docufind.app.ocr

import android.graphics.BitmapFactory
import com.docufind.app.di.IoDispatcher
import com.docufind.app.security.file.SupportedMimeType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitOcrEngine @Inject constructor(
    private val textRecognizerProvider: TextRecognizerProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OcrEngine {

    override suspend fun recognize(
        file: File,
        mimeType: String,
        onProgress: suspend (OcrProgress) -> Unit
    ): OcrResult = withContext(ioDispatcher) {
        onProgress(OcrProgress.Preparing)

        when (SupportedMimeType.fromMime(mimeType)) {
            SupportedMimeType.PDF -> {
                val helper = PdfOcrHelper { bitmap ->
                    recognizeBitmap(bitmap)
                }
                helper.recognizePdf(file, onProgress)
            }
            SupportedMimeType.JPEG, SupportedMimeType.PNG -> recognizeImageFile(file, onProgress)
            null -> OcrResult.Failure(OcrFailureReason.UNSUPPORTED_MIME)
        }
    }

    private suspend fun recognizeImageFile(
        file: File,
        onProgress: suspend (OcrProgress) -> Unit
    ): OcrResult {
        onProgress(OcrProgress.Reading)
        coroutineContext.ensureActive()

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return OcrResult.Failure(OcrFailureReason.CORRUPT_FILE)
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = computeInSampleSize(options.outWidth, options.outHeight)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: return OcrResult.Failure(OcrFailureReason.CORRUPT_FILE)

        return try {
            onProgress(OcrProgress.ProcessingPage(1, 1))
            val text = recognizeBitmap(bitmap).trim()
            onProgress(OcrProgress.Saving)
            when {
                text.isEmpty() -> OcrResult.Empty
                else -> OcrResult.Success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OcrResult.Failure(OcrFailureReason.ENGINE_ERROR, e)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun recognizeBitmap(bitmap: android.graphics.Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = textRecognizerProvider.create()
        return try {
            recognizer.processText(image)
        } finally {
            recognizer.close()
        }
    }

    private suspend fun TextRecognizer.processText(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                close()
            }
            process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text.orEmpty())
                    }
                    close()
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                    close()
                }
        }

    private fun computeInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val longest = maxOf(width, height)
        while (longest / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val MAX_IMAGE_DIMENSION = 2048
    }
}
