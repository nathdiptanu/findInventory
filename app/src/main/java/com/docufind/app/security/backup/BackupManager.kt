package com.docufind.app.security.backup

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.docufind.app.BuildConfig
import com.docufind.app.data.local.db.DocuFindDatabase
import com.docufind.app.data.local.storage.VaultStoragePaths
import com.docufind.app.security.crypto.SecureMemory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupBuildResult(
    val encryptedBytes: ByteArray,
    val preview: BackupPreview
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DocuFindDatabase,
    private val vaultStoragePaths: VaultStoragePaths,
    private val backupEncryption: BackupEncryption,
    private val backupValidator: BackupValidator
) {
    suspend fun createEncryptedBackup(
        password: CharArray,
        recordCount: Int,
        fileCount: Int,
        reminderCount: Int
    ): Result<BackupBuildResult> = withContext(Dispatchers.IO) {
        runCatching {
            checkpointDatabase()
            val createdAt = System.currentTimeMillis()
            val vaultFiles = collectVaultFiles()
            val plaintext = BackupPayloadFormat.build(
                schemaVersion = DocuFindDatabase.SCHEMA_VERSION,
                createdAtMillis = createdAt,
                recordCount = recordCount,
                reminderCount = reminderCount,
                fileCount = vaultFiles.size,
                appVersion = BuildConfig.VERSION_NAME,
                databaseBytes = readDatabaseBytes(),
                vaultFiles = vaultFiles,
                settingsBytes = readSettingsBytes()
            )
            val encrypted = backupEncryption.encrypt(plaintext, password.copyOf())
            SecureMemory.wipe(plaintext)
            val wrapped = backupValidator.wrapWithChecksum(encrypted)
            BackupBuildResult(
                encryptedBytes = wrapped,
                preview = BackupPreview(
                    schemaVersion = DocuFindDatabase.SCHEMA_VERSION,
                    fileCount = vaultFiles.size,
                    createdAtMillis = createdAt,
                    recordCount = recordCount,
                    reminderCount = reminderCount,
                    appVersion = BuildConfig.VERSION_NAME
                )
            )
        }
    }

    private fun checkpointDatabase() {
        val db: SupportSQLiteDatabase = database.openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    private fun readDatabaseBytes(): ByteArray {
        val dbFile = context.getDatabasePath(DocuFindDatabase.DATABASE_NAME)
        if (!dbFile.exists()) throw IllegalStateException("Database file missing")
        return dbFile.readBytes()
    }

    private fun collectVaultFiles(): List<VaultFileEntry> {
        val root = vaultStoragePaths.vaultDir()
        if (!root.exists()) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                VaultFileEntry(
                    relativePath = file.relativeTo(root).path.replace('\\', '/'),
                    bytes = file.readBytes()
                )
            }
            .toList()
    }

    private fun readSettingsBytes(): ByteArray {
        val prefsFile = File(context.filesDir, "datastore/docufind_preferences.preferences_pb")
        return if (prefsFile.exists()) prefsFile.readBytes() else ByteArray(0)
    }
}
