package com.docufind.app.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.docufind.app.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultRecordDaoTest {

    private lateinit var db: com.docufind.app.data.local.db.DocuFindDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DocuFindInMemoryDatabase.create(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndFetchRecord() = runBlocking {
        val record = TestFixtures.vaultRecord(id = "dao-rec-1", title = "Passport")
        db.vaultRecordDao().insert(record)
        val loaded = db.vaultRecordDao().getById("dao-rec-1")
        assertThat(loaded?.title).isEqualTo("Passport")
    }

    @Test
    fun updateRecord_persistsChanges() = runBlocking {
        val record = TestFixtures.vaultRecord(id = "dao-rec-2")
        db.vaultRecordDao().insert(record)
        db.vaultRecordDao().update(record.copy(title = "Updated title", updatedAt = 9_999L))
        assertThat(db.vaultRecordDao().getById("dao-rec-2")?.title).isEqualTo("Updated title")
    }

    @Test
    fun observeRecent_returnsNewestFirst() = runBlocking {
        db.vaultRecordDao().insert(
            TestFixtures.vaultRecord(id = "old", title = "Old").copy(updatedAt = 1_000L)
        )
        db.vaultRecordDao().insert(
            TestFixtures.vaultRecord(id = "new", title = "New").copy(updatedAt = 5_000L)
        )
        val recent = db.vaultRecordDao().observeRecent(2).first()
        assertThat(recent.first().id).isEqualTo("new")
    }

    @Test
    fun multipleFiles_linkedToSameRecord() = runBlocking {
        val record = TestFixtures.vaultRecord(id = "multi-file-rec")
        db.vaultRecordDao().insert(record)
        db.vaultFileDao().insert(TestFixtures.vaultFile(id = "f1", recordId = record.id, fileName = "a.pdf"))
        db.vaultFileDao().insert(TestFixtures.vaultFile(id = "f2", recordId = record.id, fileName = "b.pdf"))
        val files = db.vaultFileDao().getAllByRecordId(record.id)
        assertThat(files).hasSize(2)
    }
}
