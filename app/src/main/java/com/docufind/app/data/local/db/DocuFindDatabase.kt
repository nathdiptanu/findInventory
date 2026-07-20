package com.docufind.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.docufind.app.data.local.db.converter.RoomConverters
import com.docufind.app.data.local.db.dao.ActivityEventDao
import com.docufind.app.data.local.db.dao.BackupMetadataDao
import com.docufind.app.data.local.db.dao.EmergencyContactDao
import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.MedicineDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.PetRecordDao
import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.dao.SearchIndexDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.db.entity.ActivityEvent
import com.docufind.app.data.local.db.entity.BackupMetadata
import com.docufind.app.data.local.db.entity.EmergencyContact
import com.docufind.app.data.local.db.entity.FamilyMember
import com.docufind.app.data.local.db.entity.Medicine
import com.docufind.app.data.local.db.entity.Pet
import com.docufind.app.data.local.db.entity.PetRecord
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.data.local.db.entity.SearchIndexEntity
import com.docufind.app.data.local.db.entity.VaultFile
import com.docufind.app.data.local.db.entity.VaultRecord

@Database(
    entities = [
        VaultRecord::class,
        VaultFile::class,
        FamilyMember::class,
        EmergencyContact::class,
        Pet::class,
        PetRecord::class,
        Medicine::class,
        Reminder::class,
        BackupMetadata::class,
        SearchIndexEntity::class,
        ActivityEvent::class
    ],
    version = 13,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class DocuFindDatabase : RoomDatabase() {
    abstract fun vaultRecordDao(): VaultRecordDao
    abstract fun vaultFileDao(): VaultFileDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun petDao(): PetDao
    abstract fun petRecordDao(): PetRecordDao
    abstract fun medicineDao(): MedicineDao
    abstract fun reminderDao(): ReminderDao
    abstract fun backupMetadataDao(): BackupMetadataDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun activityEventDao(): ActivityEventDao

    companion object {
        const val DATABASE_NAME = "docufind.db"
        const val SCHEMA_VERSION = 13
    }
}
