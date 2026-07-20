package com.docufind.app.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.docufind.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class SupportEmailResult {
    data object Launched : SupportEmailResult()
    data class NoEmailApp(val reportText: String) : SupportEmailResult()
}

@Singleton
class SupportEmailSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosticsProvider: DeviceDiagnosticsProvider
) {
    fun buildBugReportBody(
        issueTitle: String,
        description: String,
        moduleAffected: String,
        stepsToReproduce: String
    ): String = buildString {
        appendLine("Bug Report")
        appendLine()
        appendLine("Issue title: $issueTitle")
        appendLine("Module affected: $moduleAffected")
        appendLine()
        appendLine("Description:")
        appendLine(description)
        appendLine()
        appendLine("Steps to reproduce:")
        appendLine(stepsToReproduce)
        appendLine()
        append(diagnosticsProvider.collect().toReportBlock())
    }

    fun buildFeedbackBody(message: String): String = buildString {
        appendLine("Feedback")
        appendLine()
        appendLine(message)
        appendLine()
        append(diagnosticsProvider.collect().toReportBlock())
    }

    fun sendEmail(
        subject: String,
        body: String,
        attachmentUri: Uri? = null,
        attachmentMimeType: String? = null
    ): SupportEmailResult {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SupportConstants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (attachmentUri != null) {
                type = attachmentMimeType ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("attachment", attachmentUri)
            } else {
                type = "message/rfc822"
            }
        }

        val gmailIntent = Intent(intent).setPackage(SupportConstants.GMAIL_PACKAGE)
        if (gmailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return SupportEmailResult.Launched
        }

        val chooser = Intent.createChooser(intent, null)
        if (chooser.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return SupportEmailResult.Launched
        }

        return SupportEmailResult.NoEmailApp(body)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("DocuFind support report", text))
    }

    fun prepareAttachmentFromUri(sourceUri: Uri, displayName: String): Result<Uri> = runCatching {
        val dir = File(context.cacheDir, "support_attach").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = File(dir, safeName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: error("Could not read attachment")
        FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            target
        )
    }

    fun queryAttachmentSize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0) {
                    val size = cursor.getLong(index)
                    if (size > 0) return size
                }
            }
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
    }
}
