package com.docufind.app.data.local.db.migration

import android.content.Context
import com.docufind.app.data.local.db.DocuFindDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies the encrypted database file before Room runs migrations so a failed upgrade
 * can be diagnosed without overwriting the only copy.
 */
@Singleton
class DatabaseMigrationBackup @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun backupIfExists() {
        val dbFile = context.getDatabasePath(DocuFindDatabase.DATABASE_NAME)
        if (!dbFile.exists()) return
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.filesDir, "db_migration_backups").apply { mkdirs() }
        val target = File(backupDir, "${DocuFindDatabase.DATABASE_NAME}.$stamp.bak")
        dbFile.copyTo(target, overwrite = false)
        backupDir.listFiles()
            ?.filter { it.name.startsWith(DocuFindDatabase.DATABASE_NAME) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3)
            ?.forEach { it.delete() }
    }
}
