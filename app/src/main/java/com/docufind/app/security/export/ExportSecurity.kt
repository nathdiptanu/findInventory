package com.docufind.app.security.export

import android.os.Build

/**
 * Support email payload — never includes logs, database, files, or encrypted data.
 */
data class SupportDiagnostics(
    val androidVersion: String = Build.VERSION.RELEASE,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val appVersion: String,
    val crashSummary: String? = null
) {
    fun toEmailBody(): String = buildString {
        appendLine("DocuFind Support Request")
        appendLine("------------------------")
        appendLine("Android: $androidVersion")
        appendLine("Device: $deviceModel")
        appendLine("App Version: $appVersion")
        crashSummary?.let {
            appendLine("Issue Summary: $it")
        }
        appendLine()
        appendLine("Note: No personal documents, logs, or database files are attached.")
    }
}

const val EXPORT_WARNING_MESSAGE =
    "You are exporting a decrypted copy. Store it securely. " +
        "Anyone with access to the exported file can read its contents."
