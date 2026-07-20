package com.docufind.app.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import com.docufind.app.data.local.db.DocuFindDatabase
import com.docufind.app.data.local.db.migration.DocuFindMigrations
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * JVM migration tests (no Robolectric). Verifies migration chain integrity and SQL statements.
 * Full schema round-trip tests live in androidTest — see DocuFindMigrationInstrumentedTest.
 */
class DocuFindMigrationTest {

    @Test
    fun migrationChain_coversEveryVersionUpToSchemaVersion() {
        var expectedStart = 1
        DocuFindMigrations.ALL.forEach { migration ->
            assertThat(migration.startVersion).isEqualTo(expectedStart)
            assertThat(migration.endVersion).isEqualTo(expectedStart + 1)
            expectedStart++
        }
        assertThat(expectedStart).isEqualTo(DocuFindDatabase.SCHEMA_VERSION)
    }

    @Test
    fun migrationChain_hasNoGaps() {
        assertThat(DocuFindMigrations.ALL.size).isEqualTo(DocuFindDatabase.SCHEMA_VERSION - 1)
    }

    @Test
    fun migration1To2_createsSearchIndex() {
        val db = mockDatabase()
        DocuFindMigrations.MIGRATION_1_2.migrate(db)
        verify { db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `search_index`") }) }
    }

    @Test
    fun migration8To9_addsCategoryMetadataAndActionedAt() {
        val db = mockDatabase()
        DocuFindMigrations.MIGRATION_8_9.migrate(db)
        verify { db.execSQL("ALTER TABLE vault_records ADD COLUMN categoryMetadataJson TEXT") }
        verify { db.execSQL("ALTER TABLE reminders ADD COLUMN actionedAt INTEGER") }
    }

    @Test
    fun migration10To11_sanitizesSearchIndexOnly() {
        val sql = mutableListOf<String>()
        val db = mockDatabase(sql)
        DocuFindMigrations.MIGRATION_10_11.migrate(db)
        assertThat(sql.single()).contains("UPDATE search_index")
        assertThat(sql.single()).contains("tags = NULL")
        assertThat(sql.single()).contains("notes = NULL")
        assertThat(sql.single()).contains("fileNames = NULL")
        assertThat(sql.single()).contains("searchText = lower")
    }

    @Test
    fun migration7To8_addsEmergencyEmail() {
        val db = mockDatabase()
        DocuFindMigrations.MIGRATION_7_8.migrate(db)
        verify { db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN email TEXT") }
    }

    @Test
    fun migration6To7_addsLinkedFamilyMemberId() {
        val db = mockDatabase()
        DocuFindMigrations.MIGRATION_6_7.migrate(db)
        verify { db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN linkedFamilyMemberId TEXT") }
    }

    @Test
    fun migration5To6_isNoOp() {
        val db = mockDatabase()
        DocuFindMigrations.MIGRATION_5_6.migrate(db)
        verify(exactly = 0) { db.execSQL(any()) }
    }

    @Test
    fun migration4To5_rebuildsRemindersTable() {
        val sql = mutableListOf<String>()
        val db = mockDatabase(sql)
        DocuFindMigrations.MIGRATION_4_5.migrate(db)
        assertThat(sql.any { it.contains("CREATE TABLE IF NOT EXISTS `reminders_new`") }).isTrue()
        assertThat(sql.any { it.contains("INSERT INTO reminders_new") }).isTrue()
        assertThat(sql.any { it.contains("DROP TABLE reminders") }).isTrue()
        assertThat(sql.any { it.contains("RENAME TO reminders") }).isTrue()
    }

    @Test
    fun migration2To3_migratesDocumentsToVaultRecords() {
        val sql = mutableListOf<String>()
        val db = mockDatabase(sql)
        DocuFindMigrations.MIGRATION_2_3.migrate(db)
        assertThat(sql.any { it.contains("CREATE TABLE IF NOT EXISTS `vault_records`") }).isTrue()
        assertThat(sql.any { it.contains("INSERT INTO vault_records") }).isTrue()
        assertThat(sql.any { it.contains("DROP TABLE documents") }).isTrue()
    }

    private fun mockDatabase(capturedSql: MutableList<String>? = null): SupportSQLiteDatabase {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.execSQL(any<String>()) } answers {
            capturedSql?.add(firstArg())
        }
        return db
    }
}
