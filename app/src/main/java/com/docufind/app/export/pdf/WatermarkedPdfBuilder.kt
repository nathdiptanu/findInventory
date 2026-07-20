package com.docufind.app.export.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.docufind.app.R
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class WatermarkedPdfBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun build(content: PdfExportContent, outputFile: File): Result<BuildResult> = runCatching {
        PDDocument().use { document ->
            val bodyFont = loadUnicodeFont(document)
            val boldFont = loadUnicodeFont(document, bold = true)
            val logoBitmap = loadLogoBitmap()
            val generatedAt = generationTimestamp()
            val pageLayout = PageLayout()

            var pageNumber = 0
            lateinit var stream: PDPageContentStream
            lateinit var page: PDPage
            var cursorY = 0f

            fun beginPage() {
                page = PDPage(PDRectangle.A4)
                document.addPage(page)
                pageNumber++
                stream = PDPageContentStream(document, page)
                drawWatermark(stream, document, page, logoBitmap, bodyFont)
                cursorY = PAGE_HEIGHT - MARGIN_TOP
                drawHeader(stream, bodyFont, content.documentTitle, generatedAt)
                cursorY -= HEADER_HEIGHT
            }

            fun endPage() {
                drawFooter(stream, bodyFont, pageNumber)
                stream.close()
            }

            fun ensureSpace(required: Float) {
                if (cursorY - required < MARGIN_BOTTOM + FOOTER_HEIGHT) {
                    endPage()
                    beginPage()
                }
            }

            beginPage()

            content.sections.forEach { section ->
                ensureSpace(LINE_HEIGHT * 3)
                cursorY = drawSectionHeading(stream, boldFont, section.heading, cursorY)
                section.subtitle?.let { subtitle ->
                    ensureSpace(LINE_HEIGHT * 2)
                    cursorY = drawWrappedText(stream, bodyFont, subtitle, cursorY, BODY_SIZE, italic = true)
                }
                section.fields.forEach { (label, value) ->
                    ensureSpace(LINE_HEIGHT * 3)
                    cursorY = drawField(stream, boldFont, bodyFont, label, value, cursorY)
                }
                section.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    ensureSpace(LINE_HEIGHT * 3)
                    cursorY = drawWrappedText(stream, boldFont, "Notes", cursorY, BODY_SIZE, bold = true)
                    cursorY -= LINE_HEIGHT * 0.5f
                    cursorY = drawWrappedText(stream, bodyFont, notes, cursorY, BODY_SIZE)
                }
                section.attachments.forEach { attachment ->
                    ensureSpace(LINE_HEIGHT * 4)
                    cursorY = drawWrappedText(
                        stream,
                        boldFont,
                        attachment.fileName,
                        cursorY,
                        BODY_SIZE,
                        bold = true
                    )
                    attachment.ocrText?.takeIf { it.isNotBlank() }?.let { ocr ->
                        cursorY = drawWrappedText(stream, bodyFont, ocr, cursorY, BODY_SIZE - 1)
                    }
                    val bitmaps = attachmentBitmaps(attachment)
                    bitmaps.forEach { bitmap ->
                        var placed = false
                        while (!placed) {
                            ensureSpace(pageLayout.maxImageHeight + LINE_HEIGHT)
                            val imageResult = drawImage(document, stream, bitmap, cursorY, pageLayout)
                            if (imageResult.needsNewPage) {
                                endPage()
                                beginPage()
                            } else {
                                cursorY = imageResult.cursorY
                                placed = true
                            }
                        }
                        bitmap.recycle()
                    }
                    cursorY -= LINE_HEIGHT
                }
                cursorY -= LINE_HEIGHT
            }

            endPage()
            document.save(FileOutputStream(outputFile))
        }
        BuildResult(watermarkApplied = true, pageCount = countPages(outputFile))
    }

    data class BuildResult(
        val watermarkApplied: Boolean,
        val pageCount: Int
    )

    private data class PageLayout(
        val contentWidth: Float = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT,
        val maxImageHeight: Float = PAGE_HEIGHT * 0.55f
    )

    private data class ImageDrawResult(val cursorY: Float, val needsNewPage: Boolean)

    private fun drawSectionHeading(
        stream: PDPageContentStream,
        font: PDFont,
        text: String,
        y: Float
    ): Float {
        stream.beginText()
        stream.setFont(font, HEADING_SIZE)
        stream.newLineAtOffset(MARGIN_LEFT, y)
        stream.showText(sanitizeForPdf(text))
        stream.endText()
        return y - LINE_HEIGHT * 1.5f
    }

    private fun drawField(
        stream: PDPageContentStream,
        labelFont: PDFont,
        valueFont: PDFont,
        label: String,
        value: String,
        y: Float
    ): Float {
        var cursor = y
        stream.beginText()
        stream.setFont(labelFont, BODY_SIZE)
        stream.newLineAtOffset(MARGIN_LEFT, cursor)
        stream.showText(sanitizeForPdf("$label:"))
        stream.endText()
        cursor -= LINE_HEIGHT
        return drawWrappedText(stream, valueFont, value, cursor, BODY_SIZE)
    }

    private fun drawWrappedText(
        stream: PDPageContentStream,
        font: PDFont,
        text: String,
        startY: Float,
        fontSize: Float,
        bold: Boolean = false,
        italic: Boolean = false
    ): Float {
        val usableWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
        val lines = wrapText(text, font, fontSize, usableWidth)
        var y = startY
        lines.forEach { line ->
            stream.beginText()
            stream.setFont(font, fontSize)
            stream.newLineAtOffset(MARGIN_LEFT, y)
            stream.showText(sanitizeForPdf(line))
            stream.endText()
            y -= LINE_HEIGHT
        }
        return y
    }

    private fun drawHeader(
        stream: PDPageContentStream,
        font: PDFont,
        title: String,
        generatedAt: String
    ) {
        stream.beginText()
        stream.setFont(font, TITLE_SIZE)
        stream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT - MARGIN_TOP)
        stream.showText(sanitizeForPdf(title))
        stream.endText()
        stream.beginText()
        stream.setFont(font, BODY_SIZE - 1)
        stream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT - MARGIN_TOP - LINE_HEIGHT)
        stream.showText(sanitizeForPdf("Generated: $generatedAt"))
        stream.endText()
    }

    private fun drawFooter(stream: PDPageContentStream, font: PDFont, pageNumber: Int) {
        val footerY = MARGIN_BOTTOM
        stream.beginText()
        stream.setFont(font, FOOTER_SIZE)
        stream.newLineAtOffset(MARGIN_LEFT, footerY)
        stream.showText(FOOTER_TEXT)
        stream.endText()
        val pageLabel = "Page $pageNumber"
        val labelWidth = font.getStringWidth(pageLabel) / 1000f * FOOTER_SIZE
        stream.beginText()
        stream.setFont(font, FOOTER_SIZE)
        stream.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - labelWidth, footerY)
        stream.showText(pageLabel)
        stream.endText()
    }

    private fun drawWatermark(
        stream: PDPageContentStream,
        document: PDDocument,
        page: PDPage,
        logo: Bitmap?,
        font: PDFont
    ) {
        val gs = PDExtendedGraphicsState().apply {
            nonStrokingAlphaConstant = WATERMARK_ALPHA
        }
        stream.setGraphicsStateParameters(gs)
        logo?.let { bitmap ->
            val targetWidth = PAGE_WIDTH * 0.35f
            val scale = targetWidth / bitmap.width
            val targetHeight = bitmap.height * scale
            val x = (PAGE_WIDTH - targetWidth) / 2f
            val y = (PAGE_HEIGHT - targetHeight) / 2f
            val image = LosslessFactory.createFromImage(document, bitmap)
            stream.drawImage(image, x, y, targetWidth, targetHeight)
        }
        stream.beginText()
        stream.setFont(font, 42f)
        val text = "DocuFind"
        val textWidth = font.getStringWidth(text) / 1000f * 42f
        stream.newLineAtOffset((PAGE_WIDTH - textWidth) / 2f, PAGE_HEIGHT / 2f - 20f)
        stream.showText(text)
        stream.endText()
        val reset = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 1f }
        stream.setGraphicsStateParameters(reset)
    }

    private fun drawImage(
        document: PDDocument,
        stream: PDPageContentStream,
        bitmap: Bitmap,
        cursorY: Float,
        layout: PageLayout
    ): ImageDrawResult {
        val scale = min(
            layout.contentWidth / bitmap.width.toFloat(),
            layout.maxImageHeight / bitmap.height.toFloat()
        ).coerceAtMost(1f)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        if (cursorY - height < MARGIN_BOTTOM + FOOTER_HEIGHT + LINE_HEIGHT) {
            return ImageDrawResult(cursorY, needsNewPage = true)
        }
        val image = bitmapToPdImage(document, bitmap)
        val x = MARGIN_LEFT + (layout.contentWidth - width) / 2f
        stream.drawImage(image, x, cursorY - height, width, height)
        return ImageDrawResult(cursorY - height - LINE_HEIGHT, needsNewPage = false)
    }

    private fun attachmentBitmaps(attachment: PdfExportAttachment): List<Bitmap> {
        val file = attachment.localFile
        return when {
            attachment.mimeType.startsWith("image/") -> {
                decodeOrientedBitmap(file)?.let { listOf(it) }.orEmpty()
            }
            attachment.mimeType == "application/pdf" ||
                file.extension.equals("pdf", ignoreCase = true) -> {
                renderPdfToBitmaps(file)
            }
            else -> emptyList()
        }
    }

    private fun renderPdfToBitmaps(pdfFile: File): List<Bitmap> {
        if (!pdfFile.exists()) return emptyList()
        val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val bitmaps = (0 until renderer.pageCount).mapNotNull { index ->
            runCatching {
                renderer.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }.getOrNull()
        }
        renderer.close()
        descriptor.close()
        return bitmaps
    }

    private fun bitmapToPdImage(document: PDDocument, bitmap: Bitmap): PDImageXObject {
        return if (bitmap.hasAlpha()) {
            LosslessFactory.createFromImage(document, bitmap)
        } else {
            JPEGFactory.createFromImage(document, bitmap, 0.85f)
        }
    }

    private fun decodeOrientedBitmap(file: File): Bitmap? {
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return applyExifOrientation(file, decoded)
    }

    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull() ?: return bitmap
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun loadLogoBitmap(): Bitmap? {
        // Official brand-sheet mark (same asset as launcher foreground / Home).
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_docufind_mark_raster) ?: return null
        return drawable.toBitmap(width = 256, height = 256)
    }

    private fun loadUnicodeFont(document: PDDocument, bold: Boolean = false): PDFont {
        val candidates = if (bold) {
            listOf(
                "/system/fonts/NotoSansDevanagari-Bold.ttf",
                "/system/fonts/NotoSans-Bold.ttf",
                "/system/fonts/Roboto-Bold.ttf"
            )
        } else {
            listOf(
                "/system/fonts/NotoSansDevanagari-Regular.ttf",
                "/system/fonts/NotoSans-Regular.ttf",
                "/system/fonts/Roboto-Regular.ttf",
                "/system/fonts/DroidSans.ttf"
            )
        }
        candidates.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                return PDType0Font.load(document, file)
            }
        }
        return if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
    }

    private fun wrapText(text: String, font: PDFont, fontSize: Float, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "${current} $word"
            val width = font.getStringWidth(sanitizeForPdf(candidate)) / 1000f * fontSize
            if (width <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf("") }
    }

    private fun sanitizeForPdf(text: String): String {
        return text
            .replace('\u0000', ' ')
            .replace('\n', ' ')
            .replace('\r', ' ')
    }

    private fun generationTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
        return ZonedDateTime.now().format(formatter)
    }

    private fun countPages(file: File): Int = runCatching {
        PDDocument.load(file).use { it.numberOfPages }
    }.getOrDefault(0)

    companion object {
        const val FOOTER_TEXT = "Generated securely using DocuFind"
        private val PAGE_WIDTH = PDRectangle.A4.width
        private val PAGE_HEIGHT = PDRectangle.A4.height
        private const val MARGIN_LEFT = 50f
        private const val MARGIN_RIGHT = 50f
        private const val MARGIN_TOP = 56f
        private const val MARGIN_BOTTOM = 36f
        private const val FOOTER_HEIGHT = 24f
        private const val HEADER_HEIGHT = 48f
        private const val LINE_HEIGHT = 14f
        private const val TITLE_SIZE = 16f
        private const val HEADING_SIZE = 13f
        private const val BODY_SIZE = 11f
        private const val FOOTER_SIZE = 9f
        private const val WATERMARK_ALPHA = 0.08f
    }
}
