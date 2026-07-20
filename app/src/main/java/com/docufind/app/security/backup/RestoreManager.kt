package com.docufind.app.security.backup

import android.content.Context
import com.docufind.app.data.local.db.DocuFindDatabase
import com.docufind.app.data.local.datastore.PreferencesDataStore
import com.docufind.app.data.local.storage.VaultStoragePaths
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RestoreResult {
    data class PreviewReady(val preview: BackupPreview) : RestoreResult()
    data class Success(val preview: BackupPreview) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

@Singleton
class RestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DocuFindDatabase,
    private val vaultStoragePaths: VaultStoragePaths,
    private val backupValidator: BackupValidator,
    private val preferencesDataStore: PreferencesDataStore,
    private val secureLogger: SecureLogger
) {
    suspend fun validateAndPreview(
        backupFile: ByteArray,
        password: CharArray
    ): RestoreResult = withContext(Dispatchers.IO) {
        val validation = backupValidator.validate(
            backupFile,
            password.copyOf(),
            DocuFindDatabase.SCHEMA_VERSION
        )
        if (!validation.valid || validation.decryptedPayload == null) {
            return@withContext RestoreResult.Error(
                validation.errorMessage ?: "Backup validation failed"
            )
        }
        return@withContext try {
            val parsed = BackupPayloadFormat.parseFull(validation.decryptedPayload)
            SecureMemory.wipe(validation.decryptedPayload)
            RestoreResult.PreviewReady(parsed.preview)
        } catch (e: Exception) {
            validation.decryptedPayload?.let { SecureMemory.wipe(it) }
            RestoreResult.Error("Backup file is invalid or incomplete")
        }
    }

    suspend fun restoreConfirmed(
        backupFile: ByteArray,
        password: CharArray
    ): RestoreResult = withContext(Dispatchers.IO) {
        val validation = backupValidator.validate(
            backupFile,
            password.copyOf(),
            DocuFindDatabase.SCHEMA_VERSION
        )
        if (!validation.valid || validation.decryptedPayload == null) {
            return@withContext RestoreResult.Error(
                validation.errorMessage ?: "Backup validation failed"
            )
        }

        val payloadBytes = validation.decryptedPayload
        val rollbackDir = File(context.filesDir, ROLLBACK_DIR).apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        return@withContext try {
            val parsed = BackupPayloadFormat.parseFull(payloadBytes)
            createRollback(rollbackDir)
            database.close()
            applyPayload(parsed)
            preferencesDataStore.setRestoreRestartRequired(true)
            secureLogger.info("Restore completed")
            SecureMemory.wipe(payloadBytes)
            RestoreResult.Success(parsed.preview)
        } catch (e: Exception) {
            secureLogger.error("Restore failed — rolling back")
            rollbackFrom(rollbackDir)
            SecureMemory.wipe(payloadBytes)
            RestoreResult.Error("Restore failed. Your current data was not changed.")
        } finally {
            rollbackDir.deleteRecursively()
        }
    }

    private fun createRollback(rollbackDir: File) {
        val dbFile = context.getDatabasePath(DocuFindDatabase.DATABASE_NAME)
        if (dbFile.exists()) {
            dbFile.copyTo(File(rollbackDir, DocuFindDatabase.DATABASE_NAME), overwrite = true)
        }
        val vaultRoot = vaultStoragePaths.vaultDir()
        if (vaultRoot.exists()) {
            vaultRoot.copyRecursively(File(rollbackDir, VAULT_ROLLBACK_NAME), overwrite = true)
        }
        val prefsFile = File(context.filesDir, "datastore/docufind_preferences.preferences_pb")
        if (prefsFile.exists()) {
            prefsFile.copyTo(File(rollbackDir, PREFS_ROLLBACK_NAME), overwrite = true)
        }
    }

    private fun applyPayload(parsed: ParsedBackupPayload) {
        val dbFile = context.getDatabasePath(DocuFindDatabase.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.writeBytes(parsed.databaseBytes)

        val vaultRoot = vaultStoragePaths.vaultDir()
        if (vaultRoot.exists()) vaultRoot.deleteRecursively()
        vaultRoot.mkdirs()
        parsed.vaultFiles.forEach { entry ->
            val target = File(vaultRoot, entry.relativePath)
            target.parentFile?.mkdirs()
            target.writeBytes(entry.bytes)
        }

        if (parsed.settingsBytes.isNotEmpty()) {
            val prefsFile = File(context.filesDir, "datastore/docufind_preferences.preferences_pb")
            prefsFile.parentFile?.mkdirs()
            prefsFile.writeBytes(parsed.settingsBytes)
        }
    }

    private fun rollbackFrom(rollbackDir: File) {
        val dbRollback = File(rollbackDir, DocuFindDatabase.DATABASE_NAME)
        if (dbRollback.exists()) {
            dbRollback.copyTo(
                context.getDatabasePath(DocuFindDatabase.DATABASE_NAME),
                overwrite = true
            )
        }
        val vaultRollback = File(rollbackDir, VAULT_ROLLBACK_NAME)
        if (vaultRollback.exists()) {
            val vaultRoot = vaultStoragePaths.vaultDir()
            if (vaultRoot.exists()) vaultRoot.deleteRecursively()
            vaultRollback.copyRecursively(vaultRoot, overwrite = true)
        }
        val prefsRollback = File(rollbackDir, PREFS_ROLLBACK_NAME)
        if (prefsRollback.exists()) {
            prefsRollback.copyTo(
                File(context.filesDir, "datastore/docufind_preferences.preferences_pb"),
                overwrite = true
            )
        }
    }

    companion object {
        private const val ROLLBACK_DIR = "restore_rollback"
        private const val VAULT_ROLLBACK_NAME = "vault"
        private const val PREFS_ROLLBACK_NAME = "preferences.pb"
    }
}
