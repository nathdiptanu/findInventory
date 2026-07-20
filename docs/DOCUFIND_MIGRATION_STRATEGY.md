# DocuFind — Migration Strategy

**Status:** Active (schema v9)  
**Owner:** Database Architect / Android Release Engineer

---

## Principles

1. **Never wipe user data** — `fallbackToDestructiveMigration()` is forbidden in production builds.
2. **Every version bump needs a migration** — add `Migration(N, N+1)` in `DocuFindMigrations.kt` and export schema JSON via KSP.
3. **Additive by default** — prefer `ALTER TABLE … ADD COLUMN` over table rebuilds; when rebuild is required, copy data in SQL before dropping the old table.
4. **Test every migration** — `DocuFindMigrationTest` must pass in CI before release.
5. **Backup before open** — `DatabaseMigrationBackup` copies `docufind.db` to `files/db_migration_backups/` before Room runs migrations.
6. **Fail safe** — if migration throws, show `DatabaseMigrationErrorScreen`; do not continue with a half-open database.

---

## Architecture

```
SplashViewModel
    └── DocuFindDatabaseFactory.get()
            ├── DatabaseMigrationBackup.backupIfExists()
            ├── Room.databaseBuilder + DocuFindMigrations.ALL
            ├── SQLCipher SupportOpenHelperFactory
            └── on failure → DatabaseOpenState.reportFailure()
```

| Component | Role |
|-----------|------|
| `DocuFindDatabaseFactory` | Single entry point for opening DB; caches singleton instance |
| `DocuFindMigrations` | All `Migration` objects (1→2 … 8→9) |
| `DatabaseMigrationBackup` | Pre-migration file copy; keeps last 3 backups |
| `DatabaseOpenState` | Holds migration failure for UI |
| `DatabaseMigrationErrorScreen` | Blocking error UI on splash when upgrade fails |
| `SplashViewModel` | Opens DB on cold start; defers Search/Reminder init until after successful open |

Hilt `provideDatabase()` delegates to `factory.get()`. Repositories that need the DB are injected lazily in splash startup to avoid opening the DB before the error UI can render.

---

## Version history

| Version | Migration | Summary |
|---------|-----------|---------|
| 1 | — | Initial: `documents`, `reminders` |
| 2 | 1→2 | Add `search_index` |
| 3 | 2→3 | **Major:** `documents` → `vault_records` + `vault_files`; family, emergency, pets, medicines, backup tables |
| 4 | 3→4 | Family DOB/blood group; emergency alt phone / primary; pets rebuild; pet vaccination fields; reminders + `petRecordId` |
| 5 | 4→5 | **Major:** Reminders restructured to unified engine (`linkedRecordId`, `sourceKey`, `triggerAt`, `status`, …) |
| 6 | 5→6 | No-op (schema identical; version bump only) |
| 7 | 6→7 | Emergency `linkedFamilyMemberId` |
| 8 | 7→8 | Emergency `email` |
| 9 | 8→9 | `vault_records.categoryMetadataJson`; `reminders.actionedAt` |

Full column reference: [DOCUFIND_DATABASE_SCHEMA.md](./DOCUFIND_DATABASE_SCHEMA.md)

---

## Adding a new schema version

1. Add fields to Room `@Entity` classes (nullable / default for additive columns).
2. Bump `DocuFindDatabase` `version` and `SCHEMA_VERSION`.
3. Add `MIGRATION_N_N+1` in `DocuFindMigrations.kt` and append to `ALL`.
4. Run `./gradlew assembleDebug` — KSP exports `app/schemas/.../N+1.json`.
5. Add migration test(s) in `DocuFindMigrationTest.kt` (seed old schema, migrate, assert data + new columns).
6. Update `DOCUFIND_DATABASE_SCHEMA.md` and this file.
7. Run `./gradlew test` — all migration tests must pass.

### Rules for migration SQL

- **Do not** `DROP TABLE` without copying rows first.
- **Do not** rename columns without `ALTER TABLE … RENAME COLUMN` (API 30+) or copy-and-rebuild.
- **Do** use `CREATE TABLE IF NOT EXISTS` for new tables.
- **Do** preserve encrypted `tags` and file paths — migrations touch metadata only; vault files on disk are unchanged.
- **Do** set sensible defaults when adding NOT NULL columns to existing rows.

---

## SQLCipher note

Production DB files are encrypted with SQLCipher. Migration tests use `FrameworkSQLiteOpenHelperFactory` (unencrypted) because Room migrations execute identical SQL. Encryption is applied at the file layer in production via `SupportOpenHelperFactory`.

---

## Pre-migration backup

Location: `{app filesDir}/db_migration_backups/docufind.db.{timestamp}.bak`

- Created only when `docufind.db` already exists (upgrades, not fresh install).
- Retains the 3 most recent backups; older files are deleted.
- If migration fails, user sees instructions to contact support or restore — **do not uninstall**.

---

## Failure handling

| Scenario | Behavior |
|----------|----------|
| Migration SQL throws | Factory catches, reports to `DatabaseOpenState`, splash shows error screen |
| Missing migration path | Room throws `IllegalStateException`; same error flow |
| Corrupt DB file | Error screen; backup file preserved for support diagnosis |

The app does **not** attempt auto-repair or destructive reset.

---

## Release checklist

- [ ] Schema version incremented with migration class
- [ ] `DocuFindMigrations.ALL` includes new migration
- [ ] Exported schema JSON committed
- [ ] `DocuFindMigrationTest` covers new path
- [ ] No `fallbackToDestructiveMigration` in codebase
- [ ] Manual upgrade test: install previous APK → add data → upgrade → verify vault, reminders, search
- [ ] `./gradlew assembleDebug test` passes

See also [DOCUFIND_DATA_SAFETY.md](./DOCUFIND_DATA_SAFETY.md)
