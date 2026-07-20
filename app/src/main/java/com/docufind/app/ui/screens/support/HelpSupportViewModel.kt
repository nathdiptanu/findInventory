package com.docufind.app.ui.screens.support

import androidx.lifecycle.ViewModel
import com.docufind.app.BuildConfig
import com.docufind.app.support.DeviceDiagnostics
import com.docufind.app.support.DeviceDiagnosticsProvider
import com.docufind.app.support.SupportConstants
import com.docufind.app.support.SupportEmailResult
import com.docufind.app.support.SupportEmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HelpSupportViewModel @Inject constructor(
    private val supportEmailSender: SupportEmailSender,
    diagnosticsProvider: DeviceDiagnosticsProvider
) : ViewModel() {

    val appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val diagnostics: DeviceDiagnostics = diagnosticsProvider.collect()

    fun contactSupport(): SupportEmailResult {
        val body = buildString {
            appendLine("Support request")
            appendLine()
            appendLine("Please describe your issue below:")
            appendLine()
            append(diagnostics.toReportBlock())
        }
        return supportEmailSender.sendEmail(
            subject = SupportConstants.SUPPORT_SUBJECT,
            body = body
        )
    }

    fun copyTextToClipboard(text: String) {
        supportEmailSender.copyToClipboard(text)
    }
}
