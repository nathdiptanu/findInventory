package com.docufind.app.data.local.db.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.docufind.app.data.local.db.DocuFindDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration tests using exported Room schemas.
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DocuFindMigrationInstrumentedTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DocuFindDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateAllVersions_fromV1ToV9() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            DocuFindDatabase.SCHEMA_VERSION,
            true,
            *DocuFindMigrations.ALL
        ).close()
    }

    @Test
    fun migrate2To3_preservesDocumentsAsVaultRecords() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO documents (id, title, categoryId, mimeType, filePath, encrypted, createdAt, updatedAt, notes)
                VALUES ('doc-1', 'Passport', 'documents', 'application/pdf', 'vault/passport.enc', 1, 1000, 2000, 'Travel doc')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, DocuFindMigrations.MIGRATION_2_3)
        db.query("SELECT id, title, category FROM vault_records WHERE id = 'doc-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Passport", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("documents", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        }
        db.query("SELECT id, recordId FROM vault_files WHERE recordId = 'doc-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.close()
    }

    @Test
    fun migrate8To9_addsCategoryMetadataAndActionedAt() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                """
                INSERT INTO vault_records (
                    id, title, category, subCategory, familyMemberId, petId, notes,
                    issueDate, expiryDate, renewalDate, createdAt, updatedAt, tags, isFavorite
                ) VALUES (
                    'vr-1', 'Policy', 'insurance', NULL, NULL, NULL, NULL,
                    NULL, NULL, NULL, 1000, 1000, '', 0
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, DocuFindMigrations.MIGRATION_8_9)
        db.query("SELECT categoryMetadataJson FROM vault_records WHERE id = 'vr-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("categoryMetadataJson")))
        }
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-instrumented-test"
    }
}
