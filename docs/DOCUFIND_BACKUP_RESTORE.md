# DocuFind Phase 10: Backup, Restore & Storage

Phase 10 adds encrypted local backup/restore, storage usage visibility, and an updated privacy policy screen.

## Backup

### What is included

| Component | Contents |
|-----------|----------|
| Database | SQLCipher `docufind.db` (after WAL checkpoint) |
| Vault files | All encrypted files under `files/vault/` |
| Settings | DataStore preferences (`docufind_preferences`) |
| Metadata | Record/file/reminder counts, app version, timestamp |

Reminders and metadata live in the database; settings include onboarding and app preference flags (not the device PIN keystore entry).

### Encryption

1. Payload built by `BackupPayloadFormat`
2. Encrypted with user-chosen backup password (PBKDF2 + AES-256-GCM) via `BackupEncryption`
3. CRC32 checksum wrapper via `BackupValidator.wrapWithChecksum()`
4. Saved as `.dfbackup` through Storage Access Framework — **user chooses location**

### UI (`BackupScreen`)

- Backup password + confirm fields
- Warning: **DocuFind cannot recover your backup password.**
- Create backup → system file picker → write encrypted file
- `BackupMetadata` row + last backup timestamp recorded on success

## Restore

### Validation flow

1. User picks backup file (SAF)
2. Enters backup password
3. `RestoreManager.validateAndPreview()` decrypts and parses payload
4. Preview dialog shows:
   - Records count
   - Files count
   - Reminders count
   - Backup date

### Confirmation & safety

- Restore requires vault authentication (`AuthPurpose.BACKUP_RESTORE`)
- User must confirm preview before restore runs
- **Rollback**: current DB, vault, and preferences copied to `restore_rollback/` before any write
- On failure: rollback restored — **current data is not corrupted**
- On success: database closed, files replaced, restart flag set

### After restore

Dialog message:

> Restore completed. Please restart DocuFind once to finish loading restored data.

**Restart now** relaunches the app via `AppRestartHelper`.

## Storage screen

`StorageScreen` shows:

| Stat | Source |
|------|--------|
| Total app storage used | Vault file sizes + DB file + vault directory |
| Document count | `vault_records` count |
| File count | `vault_files` count |
| Last backup date | Preferences / `BackupMetadata` |
| Backup status | Never / Backup available / Last backup failed |

## Key files

```
app/src/main/java/com/docufind/app/
├── security/backup/
│   ├── BackupPayloadFormat.kt
│   ├── BackupManager.kt
│   ├── RestoreManager.kt
│   ├── BackupEncryption.kt
│   └── BackupValidator.kt
├── data/repository/BackupRepositoryImpl.kt
├── domain/repository/BackupRepository.kt
├── ui/screens/backup/BackupScreen.kt
├── ui/screens/backup/BackupViewModel.kt
├── ui/screens/storage/StorageScreen.kt
└── ui/screens/privacy/PrivacyScreen.kt
```

## Navigation

Settings → **Backup & Restore** → `DocuFindRoutes.BACKUP`  
Settings → **Storage** → `DocuFindRoutes.STORAGE`  
Settings → **Privacy** → `DocuFindRoutes.PRIVACY`

## Build

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug test
```
