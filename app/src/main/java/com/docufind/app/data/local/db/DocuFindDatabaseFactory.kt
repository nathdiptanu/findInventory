package com.docufind.app.data.local.db

import android.content.Context
import androidx.room.Room
import com.docufind.app.data.local.db.migration.DatabaseMigrationBackup
import com.docufind.app.data.local.db.migration.DatabaseMigrationException
import com.docufind.app.data.local.db.migration.DatabaseOpenState
import com.docufind.app.data.local.db.migration.DocuFindMigrations
import com.docufind.app.security.keystore.DatabaseKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocuFindDatabaseFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseKeyManager: DatabaseKeyManager,
    private val migrationBackup: DatabaseMigrationBackup,
    private val databaseOpenState: DatabaseOpenState
) {
    @Volatile
    private var instance: DocuFindDatabase? = null

    fun get(): DocuFindDatabase {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            migrationBackup.backupIfExists()
            val db = try {
                buildDatabase()
            } catch (t: Throwable) {
                val wrapped = DatabaseMigrationException(
                    "DocuFind could not upgrade your local database safely. Your backup was preserved.",
                    t
                )
                databaseOpenState.reportFailure(wrapped)
                throw wrapped
            }
            instance = db
            return db
        }
    }

    private fun buildDatabase(): DocuFindDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = databaseKeyManager.getDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        val db = Room.databaseBuilder(
            context,
            DocuFindDatabase::class.java,
            DocuFindDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(*DocuFindMigrations.ALL)
            .build()
        db.openHelper.writableDatabase
        return db
    }
}
