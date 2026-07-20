package com.docufind.app.di

import com.docufind.app.data.local.db.DocuFindDatabase
import com.docufind.app.data.local.db.DocuFindDatabaseFactory
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
import com.docufind.app.data.repository.DocumentRepositoryImpl
import com.docufind.app.data.repository.EmergencyContactRepositoryImpl
import com.docufind.app.data.repository.FamilyMemberRepositoryImpl
import com.docufind.app.data.repository.PetRepositoryImpl
import com.docufind.app.data.repository.PreferencesRepositoryImpl
import com.docufind.app.data.repository.ReminderRepositoryImpl
import com.docufind.app.data.repository.BackupRepositoryImpl
import com.docufind.app.data.repository.SearchRepositoryImpl
import com.docufind.app.data.repository.VaultRecordRepositoryImpl
import com.docufind.app.domain.repository.DocumentRepository
import com.docufind.app.domain.repository.EmergencyContactRepository
import com.docufind.app.domain.repository.FamilyMemberRepository
import com.docufind.app.domain.repository.PetRepository
import com.docufind.app.domain.repository.PreferencesRepository
import com.docufind.app.domain.repository.ReminderRepository
import com.docufind.app.domain.repository.BackupRepository
import com.docufind.app.domain.repository.SearchRepository
import com.docufind.app.domain.repository.VaultRecordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindVaultRecordRepository(impl: VaultRecordRepositoryImpl): VaultRecordRepository

    @Binds
    @Singleton
    abstract fun bindFamilyMemberRepository(impl: FamilyMemberRepositoryImpl): FamilyMemberRepository

    @Binds
    @Singleton
    abstract fun bindEmergencyContactRepository(impl: EmergencyContactRepositoryImpl): EmergencyContactRepository

    @Binds
    @Singleton
    abstract fun bindPetRepository(impl: PetRepositoryImpl): PetRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(factory: DocuFindDatabaseFactory): DocuFindDatabase = factory.get()

    @Provides
    fun provideVaultRecordDao(database: DocuFindDatabase): VaultRecordDao = database.vaultRecordDao()

    @Provides
    fun provideVaultFileDao(database: DocuFindDatabase): VaultFileDao = database.vaultFileDao()

    @Provides
    fun provideFamilyMemberDao(database: DocuFindDatabase): FamilyMemberDao = database.familyMemberDao()

    @Provides
    fun provideEmergencyContactDao(database: DocuFindDatabase): EmergencyContactDao =
        database.emergencyContactDao()

    @Provides
    fun providePetDao(database: DocuFindDatabase): PetDao = database.petDao()

    @Provides
    fun providePetRecordDao(database: DocuFindDatabase): PetRecordDao = database.petRecordDao()

    @Provides
    fun provideMedicineDao(database: DocuFindDatabase): MedicineDao = database.medicineDao()

    @Provides
    fun provideReminderDao(database: DocuFindDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideBackupMetadataDao(database: DocuFindDatabase): BackupMetadataDao =
        database.backupMetadataDao()

    @Provides
    fun provideSearchIndexDao(database: DocuFindDatabase): SearchIndexDao = database.searchIndexDao()

    @Provides
    fun provideActivityEventDao(database: DocuFindDatabase): ActivityEventDao =
        database.activityEventDao()
}
