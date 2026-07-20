package com.docufind.app.security.protection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardProtection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyWithAutoClear(label: String, text: String, clearAfterMs: Long = CLEAR_DELAY_MS) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        secureLogger.info("Clipboard set with auto-clear scheduled")
        scope.launch {
            delay(clearAfterMs)
            clearClipboard()
        }
    }

    fun clearClipboard() {
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        secureLogger.info("Clipboard cleared")
    }

    companion object {
        private const val CLEAR_DELAY_MS = 60_000L
    }
}
