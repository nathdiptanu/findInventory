package com.docufind.app.ocr

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.docufind.app.security.file.SupportedMimeType
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

class PdfOcrHelper(
    private val pageRecognizer: suspend (Bitmap) -> String
) {
    suspend fun recognizePdf(
        file: File,
        onProgress: suspend (OcrProgress) -> Unit
    ): OcrResult {
        onProgress(OcrProgress.Reading)

        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        return try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = try {
                PdfRenderer(descriptor)
            } catch (e: SecurityException) {
                return OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
            } catch (e: IOException) {
                if (isPasswordProtected(e)) {
                    return OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
                }
                return OcrResult.Failure(OcrFailureReason.CORRUPT_FILE, e)
            }

            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                return OcrResult.Empty
            }

            val pagesToProcess = minOf(pageCount, MAX_PAGES)
            val textParts = ArrayList<String>(pagesToProcess)

            for (pageIndex in 0 until pagesToProcess) {
                coroutineContext.ensureActive()
                onProgress(OcrProgress.ProcessingPage(pageIndex + 1, pagesToProcess))

                var page: PdfRenderer.Page? = null
                var bitmap: Bitmap? = null
                try {
                    page = renderer.openPage(pageIndex)
                    bitmap = renderScaledBitmap(page)
                    val pageText = pageRecognizer(bitmap).trim()
                    if (pageText.isNotEmpty()) {
                        textParts.add(pageText)
                    }
                } catch (e: SecurityException) {
                    return OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
                } catch (e: IOException) {
                    if (isPasswordProtected(e)) {
                        return OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
                    }
                    return OcrResult.Failure(OcrFailureReason.CORRUPT_FILE, e)
                } finally {
                    bitmap?.recycle()
                    page?.close()
                }
            }

            onProgress(OcrProgress.Saving)
            val combined = combinePageText(textParts)
            when {
                combined.isEmpty() -> OcrResult.Empty
                else -> OcrResult.Success(combined)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
        } catch (e: IOException) {
            if (isPasswordProtected(e)) {
                OcrResult.Failure(OcrFailureReason.PASSWORD_PROTECTED_PDF, e)
            } else {
                OcrResult.Failure(OcrFailureReason.CORRUPT_FILE, e)
            }
        } catch (e: Exception) {
            OcrResult.Failure(OcrFailureReason.ENGINE_ERROR, e)
        } finally {
            renderer?.close()
            descriptor?.close()
        }
    }

    private fun renderScaledBitmap(page: PdfRenderer.Page): Bitmap {
        val scale = computeScale(page.width, page.height)
        val width = (page.width * scale).toInt().coerceAtLeast(1)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    private fun computeScale(pageWidth: Int, pageHeight: Int): Float {
        val longestSide = maxOf(pageWidth, pageHeight).toFloat()
        if (longestSide <= MAX_BITMAP_DIMENSION) return 1f
        return MAX_BITMAP_DIMENSION / longestSide
    }

    private fun isPasswordProtected(error: Throwable): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return message.contains("password") ||
            message.contains("encrypted") ||
            message.contains("needs password") ||
            message.contains("cannot be opened")
    }

    companion object {
        const val MAX_PAGES = 10
        private const val MAX_BITMAP_DIMENSION = 2048f

        internal fun combinePageText(parts: List<String>): String =
            parts.filter { it.isNotBlank() }.joinToString(separator = "\n\n").trim()
    }
}

fun pdfExtensionForMime(mimeType: String): String =
    when (SupportedMimeType.fromMime(mimeType)) {
        SupportedMimeType.PDF -> "pdf"
        SupportedMimeType.PNG -> "png"
        SupportedMimeType.JPEG -> "jpg"
        null -> "bin"
    }
