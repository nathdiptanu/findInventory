package com.docufind.app.ui.preview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.min

object PdfPreviewRenderer {
    fun renderPages(file: File, maxPages: Int = 20): List<Bitmap> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val pageCount = min(renderer.pageCount, maxPages)
        val bitmaps = (0 until pageCount).mapNotNull { index ->
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
}
