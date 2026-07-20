package com.docufind.app.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for docufind.db (SQLCipher).
 * Every version bump must add a migration here — never use destructive fallback.
 */
object DocuFindMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_index` (
                    `documentId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `tags` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`documentId`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_index_title` ON `search_index` (`title`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_index_categoryId` ON `search_index` (`categoryId`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vault_records` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `subCategory` TEXT,
                    `familyMemberId` TEXT,
                    `petId` TEXT,
                    `notes` TEXT,
                    `issueDate` INTEGER,
                    `expiryDate` INTEGER,
                    `renewalDate` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `tags` TEXT NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_records_category` ON `vault_records` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_records_updatedAt` ON `vault_records` (`updatedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_records_familyMemberId` ON `vault_records` (`familyMemberId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_records_petId` ON `vault_records` (`petId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_records_isFavorite` ON `vault_records` (`isFavorite`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vault_files` (
                    `id` TEXT NOT NULL,
                    `recordId` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `fileSize` INTEGER NOT NULL,
                    `localPath` TEXT NOT NULL,
                    `thumbnailPath` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recordId`) REFERENCES `vault_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_files_recordId` ON `vault_files` (`recordId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `family_members` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `relationship` TEXT NOT NULL,
                    `phone` TEXT,
                    `email` TEXT,
                    `notes` TEXT,
                    `avatarPath` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_family_members_name` ON `family_members` (`name`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `emergency_contacts` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT NOT NULL,
                    `relationship` TEXT,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_emergency_contacts_name` ON `emergency_contacts` (`name`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pets` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `species` TEXT,
                    `breed` TEXT,
                    `birthDate` INTEGER,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pets_name` ON `pets` (`name`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pet_records` (
                    `id` TEXT NOT NULL,
                    `petId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `recordType` TEXT NOT NULL,
                    `notes` TEXT,
                    `recordDate` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`petId`) REFERENCES `pets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_records_petId` ON `pet_records` (`petId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_records_recordType` ON `pet_records` (`recordType`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `medicines` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `dosage` TEXT,
                    `familyMemberId` TEXT,
                    `petId` TEXT,
                    `scheduleNotes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_medicines_name` ON `medicines` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_medicines_familyMemberId` ON `medicines` (`familyMemberId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_medicines_petId` ON `medicines` (`petId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `backup_metadata` (
                    `id` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `localPath` TEXT NOT NULL,
                    `fileSize` INTEGER NOT NULL,
                    `recordCount` INTEGER NOT NULL,
                    `checksumSha256` TEXT,
                    `appVersion` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_backup_metadata_createdAt` ON `backup_metadata` (`createdAt`)")

            // Migrate documents -> vault_records + vault_files
            db.execSQL(
                """
                INSERT INTO vault_records (
                    id, title, category, subCategory, familyMemberId, petId, notes,
                    issueDate, expiryDate, renewalDate, createdAt, updatedAt, tags, isFavorite
                )
                SELECT
                    id, title, categoryId, NULL, NULL, NULL, notes,
                    NULL, NULL, NULL, createdAt, updatedAt, '', 0
                FROM documents
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO vault_files (
                    id, recordId, fileName, mimeType, fileSize, localPath, thumbnailPath, createdAt
                )
                SELECT
                    id, id, title, mimeType, 0, filePath, NULL, createdAt
                FROM documents
                """.trimIndent()
            )

            // Reminders: recreate with expanded schema
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminders_new` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `recordId` TEXT,
                    `medicineId` TEXT,
                    `dueDate` INTEGER NOT NULL,
                    `frequency` TEXT NOT NULL,
                    `completed` INTEGER NOT NULL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO reminders_new (id, title, recordId, medicineId, dueDate, frequency, completed, notes, createdAt)
                SELECT id, title, categoryId, NULL, dueDate, frequency, completed, NULL, createdAt
                FROM reminders
                """.trimIndent()
            )
            db.execSQL("DROP TABLE reminders")
            db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_dueDate` ON `reminders` (`dueDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_recordId` ON `reminders` (`recordId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_medicineId` ON `reminders` (`medicineId`)")

            // search_index: documentId -> recordId
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_index_new` (
                    `recordId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `tags` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`recordId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO search_index_new (recordId, title, category, tags, updatedAt)
                SELECT documentId, title, categoryId, tags, updatedAt FROM search_index
                """.trimIndent()
            )
            db.execSQL("DROP TABLE search_index")
            db.execSQL("ALTER TABLE search_index_new RENAME TO search_index")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_index_title` ON `search_index` (`title`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_index_category` ON `search_index` (`category`)")

            db.execSQL("DROP TABLE documents")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE family_members ADD COLUMN dateOfBirth INTEGER")
            db.execSQL("ALTER TABLE family_members ADD COLUMN bloodGroup TEXT")

            db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN alternatePhone TEXT")
            db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN isPrimary INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE pet_records ADD COLUMN vaccineName TEXT")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN nextDueDate INTEGER")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN vetClinic TEXT")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN attachmentPath TEXT")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN attachmentMimeType TEXT")
            db.execSQL("ALTER TABLE pet_records ADD COLUMN attachmentName TEXT")

            db.execSQL("ALTER TABLE reminders ADD COLUMN petRecordId TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_petRecordId` ON `reminders` (`petRecordId`)")

            // Rebuild pets table with expanded columns
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pets_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `petType` TEXT,
                    `breed` TEXT,
                    `gender` TEXT,
                    `birthDate` INTEGER,
                    `weight` TEXT,
                    `color` TEXT,
                    `microchipId` TEXT,
                    `vetName` TEXT,
                    `vetPhone` TEXT,
                    `notes` TEXT,
                    `photoPath` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO pets_new (
                    id, name, petType, breed, gender, birthDate, weight, color,
                    microchipId, vetName, vetPhone, notes, photoPath, createdAt, updatedAt
                )
                SELECT
                    id, name, species, breed, NULL, birthDate, NULL, NULL,
                    NULL, NULL, NULL, notes, NULL, createdAt, updatedAt
                FROM pets
                """.trimIndent()
            )
            db.execSQL("DROP TABLE pets")
            db.execSQL("ALTER TABLE pets_new RENAME TO pets")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pets_name` ON `pets` (`name`)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminders_new` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `linkedRecordId` TEXT,
                    `linkedPetId` TEXT,
                    `linkedFamilyMemberId` TEXT,
                    `linkedPetRecordId` TEXT,
                    `linkedMedicineId` TEXT,
                    `category` TEXT NOT NULL,
                    `reminderDate` INTEGER NOT NULL,
                    `reminderTimeMinutes` INTEGER NOT NULL,
                    `triggerAt` INTEGER NOT NULL,
                    `repeatType` TEXT NOT NULL,
                    `importance` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `sourceKey` TEXT NOT NULL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO reminders_new (
                    id, title, linkedRecordId, linkedPetId, linkedFamilyMemberId, linkedPetRecordId,
                    linkedMedicineId, category, reminderDate, reminderTimeMinutes, triggerAt,
                    repeatType, importance, status, sourceKey, notes, createdAt
                )
                SELECT
                    id,
                    title,
                    recordId,
                    NULL,
                    NULL,
                    petRecordId,
                    medicineId,
                    'CUSTOM',
                    dueDate,
                    540,
                    dueDate,
                    CASE
                        WHEN frequency IN ('daily', 'DAILY') THEN 'DAILY'
                        WHEN frequency IN ('weekly', 'WEEKLY') THEN 'WEEKLY'
                        WHEN frequency IN ('monthly', 'MONTHLY') THEN 'MONTHLY'
                        WHEN frequency IN ('yearly', 'YEARLY') THEN 'YEARLY'
                        ELSE 'NO_REPEAT'
                    END,
                    'NORMAL',
                    CASE WHEN completed = 1 THEN 'COMPLETED' ELSE 'ACTIVE' END,
                    'migrated:' || id,
                    notes,
                    createdAt
                FROM reminders
                """.trimIndent()
            )
            db.execSQL("DROP TABLE reminders")
            db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminders_sourceKey` ON `reminders` (`sourceKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_triggerAt` ON `reminders` (`triggerAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linkedRecordId` ON `reminders` (`linkedRecordId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linkedPetRecordId` ON `reminders` (`linkedPetRecordId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linkedMedicineId` ON `reminders` (`linkedMedicineId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_status` ON `reminders` (`status`)")
        }
    }

    /** Schema identical to v5; version bump only. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN linkedFamilyMemberId TEXT")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN email TEXT")
        }
    }

    /** Additive columns for category metadata and reminder action timestamps. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE vault_records ADD COLUMN categoryMetadataJson TEXT")
            db.execSQL("ALTER TABLE reminders ADD COLUMN actionedAt INTEGER")
        }
    }

    /**
     * Sanitizes the derived search cache. Real vault records are untouched.
     * Notes and file names can contain sensitive context and must not be stored in search_index.
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE search_index SET notes = NULL, fileNames = NULL")
        }
    }

    /**
     * Re-sanitizes the derived search cache for existing v10 users.
     * `vault_records.tags` can contain encrypted metadata markers (`meta:*`) and must not be mirrored
     * into the searchable cache. Vault records and encrypted files are untouched.
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE search_index
                SET
                    tags = NULL,
                    notes = NULL,
                    fileNames = NULL,
                    searchText = lower(trim(
                        coalesce(title, '') || ' ' ||
                        coalesce(category, '') || ' ' ||
                        coalesce(subCategory, '') || ' ' ||
                        coalesce(familyMemberName, '') || ' ' ||
                        coalesce(petName, '')
                    ))
                """.trimIndent()
            )
        }
    }

    /**
     * Adds local-only Activity Insights. Events stay inside the encrypted SQLCipher DB.
     * The table stores coarse usage facts only and never stores document titles, notes, file names, or search text.
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_events` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `durationMs` INTEGER,
                    `screen` TEXT,
                    `category` TEXT,
                    `metadata` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_type` ON `activity_events` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_timestamp` ON `activity_events` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_screen` ON `activity_events` (`screen`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_category` ON `activity_events` (`category`)")
        }
    }

    /**
     * Soft-delete / Trash: keeps encrypted files until the user permanently deletes.
     */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `vault_records` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_vault_records_deletedAt` ON `vault_records` (`deletedAt`)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13
    )
}
