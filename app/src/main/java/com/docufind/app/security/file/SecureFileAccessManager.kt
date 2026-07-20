package com.docufind.app.security.file

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.docufind.app.BuildConfig
import com.docufind.app.data.local.storage.VaultFileExporter
import com.docufind.app.export.pdf.PdfExportManager
import com.docufind.app.export.pdf.PdfExportRequest
import com.docufind.app.export.pdf.PdfExportResult
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.AuthPurpose
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.session.VaultSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SecureFileAccessResult {
    data class Ready(val file: File, val mimeType: String, val displayName: String) : SecureFileAccessResult()
    data class AuthRequired(val message: String) : SecureFileAccessResult()
    data class Error(val message: String) : SecureFileAccessResult()
}

@Singleton
class SecureFileAccessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRecordRepository: VaultRecordRepository,
    private val vaultFileExporter: VaultFileExporter,
    private val authGate: AuthGate,
    private val vaultSessionManager: VaultSessionManager,
    private val pdfExportManager: PdfExportManager
) {
    suspend fun decryptFile(
        activity: FragmentActivity,
        fileId: String,
        pin: CharArray? = null
    ): SecureFileAccessResult = decryptFileForView(activity, fileId, pin)

    suspend fun decryptFileForView(
        activity: FragmentActivity,
        fileId: String,
        pin: CharArray? = null
    ): SecureFileAccessResult = withContext(Dispatchers.IO) {
        if (!vaultSessionManager.isUnlocked.value) {
            val auth = authGate.authenticateForPurpose(activity, AuthPurpose.DOCUMENT_VIEW, pin)
            when (auth) {
                is AuthResult.Success -> Unit
                is AuthResult.Cancelled -> return@withContext SecureFileAccessResult.AuthRequired("Authentication cancelled.")
                is AuthResult.Error -> return@withContext SecureFileAccessResult.AuthRequired(auth.message)
                AuthResult.Failed -> return@withContext SecureFileAccessResult.AuthRequired("Authentication failed.")
            }
        }
        decryptToCache(fileId)
    }

    suspend fun decryptFileForExport(
        activity: FragmentActivity,
        fileId: String,
        pin: CharArray? = null
    ): SecureFileAccessResult = withContext(Dispatchers.IO) {
        val auth = authGate.authenticateForPurpose(activity, AuthPurpose.DOCUMENT_EXPORT, pin)
        when (auth) {
            is AuthResult.Success -> Unit
            is AuthResult.Cancelled -> return@withContext SecureFileAccessResult.AuthRequired("Authentication cancelled.")
            is AuthResult.Error -> return@withContext SecureFileAccessResult.AuthRequired(auth.message)
            AuthResult.Failed -> return@withContext SecureFileAccessResult.AuthRequired("Authentication failed.")
        }
        decryptToCache(fileId)
    }

    private suspend fun decryptToCache(fileId: String): SecureFileAccessResult {
        val decrypted = vaultRecordRepository.decryptFileForShare(fileId)
        return decrypted.fold(
            onSuccess = { file ->
                val mime = guessMime(file)
                SecureFileAccessResult.Ready(file, mime, file.name)
            },
            onFailure = {
                val message = if (it is FileNotFoundException || it is CorruptedFileException) {
                    "File unavailable. The original file may have been moved or deleted."
                } else {
                    "Preview unavailable. You can open or share this file."
                }
                SecureFileAccessResult.Error(message)
            }
        )
    }

    fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setType(mimeType)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openFile(file: File, mimeType: String): Result<Unit> = runCatching {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun exportPdf(
        activity: FragmentActivity,
        request: PdfExportRequest,
        pin: CharArray? = null
    ): PdfExportResult = pdfExportManager.export(activity, request, pin)

    fun shareExportedPdf(file: File) {
        shareFile(file, "application/pdf")
    }

    fun wipeExportCache() {
        pdfExportManager.wipeExportCache()
    }

    suspend fun exportToDownloads(file: File, displayName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloads.mkdirs()
            val target = File(downloads, displayName)
            file.copyTo(target, overwrite = true)
            target
        }
    }

    private fun guessMime(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".pdf") -> "application/pdf"
            name.endsWith(".png") -> "image/png"
            else -> "image/jpeg"
        }
    }
}
