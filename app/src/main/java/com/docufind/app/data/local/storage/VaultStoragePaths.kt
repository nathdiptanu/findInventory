package com.docufind.app.data.local.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultStoragePaths @Inject constructor(
    @ApplicationContext context: Context
) {
    private val filesRoot = context.filesDir

    fun vaultDir(): File = File(filesRoot, VaultStorageConfig.VAULT_DIR_NAME).apply { mkdirs() }

    fun tempImportDir(): File = File(filesRoot, VaultStorageConfig.TEMP_DIR_NAME).apply { mkdirs() }

    fun thumbnailDir(): File = File(vaultDir(), VaultStorageConfig.THUMB_DIR_NAME).apply { mkdirs() }

    fun createTempImportFile(): File {
        tempImportDir().listFiles()?.forEach { stale ->
            if (stale.isFile && stale.lastModified() < System.currentTimeMillis() - TEMP_MAX_AGE_MS) {
                stale.delete()
            }
        }
        return File.createTempFile("import_", ".tmp", tempImportDir())
    }

    fun relativePath(base: File, file: File): String =
        file.relativeTo(base).path.replace('\\', '/')

    fun resolveRelative(base: File, relativePath: String): File = File(base, relativePath)

    companion object {
        private const val TEMP_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
