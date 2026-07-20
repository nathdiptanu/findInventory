package com.docufind.app.data.local.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.docufind.app.security.file.SecureDelete
import com.docufind.app.security.file.SupportedMimeType
import com.docufind.app.security.logging.SecureLogger
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultThumbnailGenerator @Inject constructor(
    private val storagePaths: VaultStoragePaths,
    private val secureLogger: SecureLogger
) {
    fun generateThumbnail(
        sourceFile: File,
        mimeType: String,
        fileId: String
    ): String? {
        if (SupportedMimeType.fromMime(mimeType) !in IMAGE_TYPES) return null
        if (!sourceFile.exists() || !sourceFile.canRead()) return null

        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sampleSize = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                VaultStorageConfig.THUMBNAIL_MAX_DIMENSION
            )
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
                ?: return null

            val thumbFile = File(storagePaths.thumbnailDir(), "$fileId.jpg")
            try {
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                storagePaths.relativePath(storagePaths.vaultDir(), thumbFile)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            secureLogger.warn("Thumbnail generation failed: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / sampleSize >= maxDimension || halfHeight / sampleSize >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    companion object {
        private val IMAGE_TYPES = setOf(SupportedMimeType.JPEG, SupportedMimeType.PNG)
    }

    fun deleteThumbnail(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        val file = storagePaths.resolveRelative(storagePaths.vaultDir(), relativePath)
        SecureDelete.wipeFile(file)
    }
}
