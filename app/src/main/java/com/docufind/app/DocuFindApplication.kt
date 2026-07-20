package com.docufind.app

import android.app.Application
import android.util.Log
import com.docufind.app.BuildConfig
import com.docufind.app.ocr.OcrTempStore
import com.docufind.app.security.file.SecureFileCache
import com.docufind.app.security.session.AppLifecycleObserver
import com.docufind.app.security.session.VaultSessionManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class DocuFindApplication : Application() {

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var vaultSessionManager: VaultSessionManager
    @Inject lateinit var secureFileCache: SecureFileCache
    @Inject lateinit var ocrTempStore: OcrTempStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initPdfExportSafely()
        appLifecycleObserver.register()
        vaultSessionManager.isUnlocked
            .onEach { unlocked ->
                if (!unlocked) {
                    runCatching { secureFileCache.wipeAll() }
                    runCatching { ocrTempStore.wipeAll() }
                }
            }
            .launchIn(appScope)
    }

    /**
     * PDF export helpers must never crash cold start. Fonts/crypto init is best-effort;
     * export screens surface failures if PdfBox is unavailable.
     */
    private fun initPdfExportSafely() {
        runCatching {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
            PDFBoxResourceLoader.init(this)
        }.onFailure { error ->
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "PdfBox init deferred: ${error.javaClass.simpleName}")
            }
        }
    }

    companion object {
        private const val TAG = "DocuFindApp"
    }
}
