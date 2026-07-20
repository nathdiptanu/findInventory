# DocuFind — Data Safety

**Principle:** User data must survive every app update. DocuFind stores sensitive personal documents locally; data loss is unacceptable.

---

## Storage layers

| Layer | Location | Encryption | Survives app update? |
|-------|----------|------------|----------------------|
| Room DB | `docufind.db` | SQLCipher (passphrase from Android Keystore) | Yes — with migrations |
| Vault files | App-private `files/vault/` | AES-GCM per file | Yes — paths stored in DB |
| Thumbnails | App-private cache | Encrypted or derived | Regeneratable |
| Preferences | DataStore `docufind_preferences` | OS sandbox | Yes — separate from Room |
| PIN / keys | EncryptedSharedPreferences + Keystore | Hardware-backed where available | Yes |

Room migrations **never** delete vault files on disk. File encryption keys are independent of schema version.

---

## Sensitive field handling

These values must never appear in plaintext in the DB, logs, or crash reports:

| Field type | Storage |
|------------|---------|
| Card numbers | Encrypted in `vault_records.tags` via `SensitiveMetadataCipher` |
| Passwords | Encrypted in tags |
| Netbanking username/password | Encrypted in tags |
| Policy numbers | Encrypted in tags |
| File content | Encrypted on disk (`.enc`); DB stores path + metadata only |

Non-sensitive category-specific fields may use `vault_records.categoryMetadataJson` (plain JSON). Do **not** put secrets in this column.

---

## Upgrade safety guarantees

1. **No destructive migration** — removed `fallbackToDestructiveMigration()`.
2. **Explicit migration chain** — versions 1→9 fully covered in `DocuFindMigrations.kt`.
3. **Pre-migration backup** — encrypted DB file copied before Room opens on upgrade.
4. **Safe failure UI** — migration errors block the app with guidance; DB file is not overwritten after backup.
5. **Automated tests** — `DocuFindMigrationTest` validates schema paths and data preservation.

---

## What is NOT in Room

Stored in DataStore instead (safe across DB migrations):

- Onboarding completed
- User profile name / mobile / email
- PIN configured flag, biometric preference
- Vault lock state
- Tagline / How-to-use UI state
- Recent searches, backup timestamps

See [DOCUFIND_ONBOARDING.md](./DOCUFIND_ONBOARDING.md)

---

## User actions on migration failure

The error screen instructs users to:

1. **Do not uninstall** — encrypted DB and vault files remain on device.
2. Contact support (Help & Support → Report a bug) with diagnostics.
3. Restore from a local backup if one exists ([DOCUFIND_BACKUP_RESTORE.md](./DOCUFIND_BACKUP_RESTORE.md)).

Support can use `files/db_migration_backups/` copies for diagnosis.

---

## Developer guidelines

- Before renaming or removing a column: ship a multi-release deprecation (add new column → migrate data → remove old in later version).
- Never clear `files/vault/` during migration or app update.
- Never log decrypted tags, PINs, or file paths in production.
- Run manual upgrade test before every release (see [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md)).

---

## Validation

```bash
./gradlew assembleDebug test
```

Manual:

1. Install previous release APK (or create DB at older schema via migration test helper).
2. Add vault records with encrypted sensitive tags, attachments, reminders.
3. Upgrade to current build.
4. Unlock vault — records and files accessible.
5. Reminders appear in Upcoming; actioned state preserved.
6. Search finds existing records.
