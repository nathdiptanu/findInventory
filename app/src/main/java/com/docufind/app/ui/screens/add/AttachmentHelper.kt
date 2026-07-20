package com.docufind.app.ui.screens.add

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.docufind.app.R
import com.docufind.app.security.file.MAX_VAULT_FILE_BYTES
import com.docufind.app.security.file.SupportedMimeType
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

sealed class AttachmentResolveResult {
    data class Success(val info: PendingAttachmentInfo) : AttachmentResolveResult()
    data class Error(val messageResId: Int) : AttachmentResolveResult()
}

object AttachmentHelper {
    private const val PREVIEW_DIR = "add_preview"

    fun resolveAttachment(
        context: Context,
        uri: Uri,
        fallbackName: String = "document",
        previewId: String = UUID.randomUUID().toString()
    ): AttachmentResolveResult {
        val mimeType = context.contentResolver.getType(uri)
            ?.takeIf { SupportedMimeType.fromMime(it) != null }
            ?: inferMimeFromName(fallbackName)
            ?: return AttachmentResolveResult.Error(R.string.unsupported_file_type)

        val displayName = queryDisplayName(context, uri) ?: fallbackName
        val sizeBytes = querySize(context, uri)

        if (sizeBytes > MAX_VAULT_FILE_BYTES) {
            return AttachmentResolveResult.Error(R.string.file_too_large)
        }

        val previewPath = try {
            generatePreviewPath(context, uri, mimeType, previewId)
        } catch (_: Exception) {
            null
        }

        return AttachmentResolveResult.Success(
            PendingAttachmentInfo(
                uri = uri.toString(),
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                localPreviewPath = previewPath
            )
        )
    }

    private fun inferMimeFromName(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pdf") -> SupportedMimeType.PDF.mime
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> SupportedMimeType.JPEG.mime
            lower.endsWith(".png") -> SupportedMimeType.PNG.mime
            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) return cursor.getString(index)
            }
        return null
    }

    private fun querySize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0) {
                    val size = cursor.getLong(index)
                    if (size > 0) return size
                }
            }
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
    }

    private fun generatePreviewPath(
        context: Context,
        uri: Uri,
        mimeType: String,
        previewId: String
    ): String? {
        if (SupportedMimeType.fromMime(mimeType) == SupportedMimeType.PDF) return null
        val dir = File(context.cacheDir, PREVIEW_DIR).apply { mkdirs() }
        val previewFile = File(dir, "preview_$previewId.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeStream(input, null, options) ?: return null
            FileOutputStream(previewFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
        }
        return previewFile.absolutePath
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes <= 0 -> "Unknown size"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun parseTags(text: String): List<String> =
        text.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

data class PendingAttachmentInfo(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPreviewPath: String?
)
