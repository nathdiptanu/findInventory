package com.docufind.app.cloud

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.docufind.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleVaultAccount(
    val email: String,
    val displayName: String? = null
)

/**
 * Google account + Drive export for encrypted DocuFind backups.
 *
 * - Account: system Google account picker (changeable anytime)
 * - Drive: shares the already-encrypted `.dfbackup` into the Google Drive app / SAF
 *   so the vault password never leaves the device.
 */
@Singleton
class GoogleAccountSession @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("docufind_google", Context.MODE_PRIVATE)

    fun getSignedInAccount(): GoogleVaultAccount? {
        val email = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() } ?: return null
        return GoogleVaultAccount(email = email, displayName = prefs.getString(KEY_NAME, null))
    }

    fun getAccountPickerIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    fun onAccountPicked(email: String?) {
        if (email.isNullOrBlank()) return
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun switchAccountIntent(): Intent {
        clearAccount()
        return getAccountPickerIntent()
    }

    fun clearAccount() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_EMAIL = "google_email"
        private const val KEY_NAME = "google_name"
    }
}

@Singleton
class GoogleDriveBackupClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Writes encrypted bytes to a cache file and returns a share Intent aimed at Google Drive
     * (falls back to a generic share sheet if Drive is not installed).
     */
    suspend fun createDriveShareIntent(
        encryptedBytes: ByteArray,
        fileName: String = BACKUP_FILE_NAME
    ): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "drive_share").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(encryptedBytes)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "DocuFind encrypted vault backup. Unlock only with your DocuFind backup password."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(DRIVE_PACKAGE)
            }
            if (driveIntent.resolveActivity(context.packageManager) != null) {
                driveIntent
            } else {
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Save encrypted backup to Google Drive"
                )
            }
        }
    }

    companion object {
        const val BACKUP_FILE_NAME = "docufind-vault.dfbackup"
        private const val DRIVE_PACKAGE = "com.google.android.apps.docs"
    }
}
