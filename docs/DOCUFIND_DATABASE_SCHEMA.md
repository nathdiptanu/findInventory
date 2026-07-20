# DocuFind — Database Schema

Room database: **`docufind.db`** (version **12**, SQLCipher encrypted)

Schema export path: `app/schemas/com.docufind.app.data.local.db.DocuFindDatabase/` (generated at build time)

All data is **local-only**. No server, no cloud sync.

Migration policy: explicit migrations only — see [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md).

---

## Entity Relationship

```
vault_records (1) ──< vault_files (N)
vault_records ── optional ──> family_members
vault_records ── optional ──> pets
pet_records (N) ──> pets (1)
medicines ── optional ──> family_members / pets
reminders ── optional ──> vault_records / medicines
search_index ── mirrors vault_records metadata (title, category, tags)
backup_metadata ── local backup file registry
activity_events ── local-only coarse activity insights
```

Foreign keys:

- `vault_files.recordId` → `vault_records.id` (CASCADE delete)
- `pet_records.petId` → `pets.id` (CASCADE delete)

---

## Table: `vault_records`

Core document / record metadata. File bytes live in encrypted vault storage (`vault_files`).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | UUID string |
| `title` | TEXT | NOT NULL | Display name |
| `category` | TEXT | NOT NULL, INDEX | e.g. `documents`, `id_cards`, `medical` |
| `subCategory` | TEXT | NULL | e.g. `aadhaar`, `prescriptions` |
| `familyMemberId` | TEXT | NULL, INDEX | Optional link to family member |
| `petId` | TEXT | NULL, INDEX | Optional link to pet profile |
| `notes` | TEXT | NULL | User notes |
| `issueDate` | INTEGER | NULL | Epoch millis |
| `expiryDate` | INTEGER | NULL | Epoch millis |
| `renewalDate` | INTEGER | NULL | Epoch millis |
| `createdAt` | INTEGER | NOT NULL | Epoch millis |
| `updatedAt` | INTEGER | NOT NULL, INDEX | Epoch millis |
| `tags` | TEXT | NOT NULL | List via TypeConverter; **encrypted sensitive values** stored here |
| `isFavorite` | INTEGER | NOT NULL, INDEX | Boolean |
| `categoryMetadataJson` | TEXT | NULL | Non-sensitive category-specific fields (JSON) |

Sensitive values (card numbers, passwords, policy numbers) are encrypted into `tags` via `SensitiveMetadataCipher` — not stored in `categoryMetadataJson`.

**Banking (`finance`) and Property (`property`) metadata** use the same tags/JSON pattern as other categories. Added in the 2026-07-20 production-readiness pass — **no Room schema version bump** required (still v12).

---

- `observeCountByCategory(category)` → `Flow<Int>`
- `observeTotalCount()` → `Flow<Int>`
- `observeByCategory(category)` → `Flow<List<VaultRecord>>`
- `observeRecent(limit)` → `Flow<List<VaultRecord>>`
- `observeFavorites()` → `Flow<List<VaultRecord>>`

---

## Table: `vault_files`

Encrypted file attachments for a vault record.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | UUID; matches encrypted file id on disk |
| `recordId` | TEXT | NOT NULL, FK, INDEX | Parent vault record |
| `fileName` | TEXT | NOT NULL | Original display name |
| `mimeType` | TEXT | NOT NULL | `application/pdf`, `image/jpeg`, `image/png` |
| `fileSize` | INTEGER | NOT NULL | Original plaintext size in bytes |
| `localPath` | TEXT | NOT NULL | Relative path under vault dir (`.enc` file) |
| `thumbnailPath` | TEXT | NULL | Relative path to JPEG thumbnail |
| `createdAt` | INTEGER | NOT NULL | Epoch millis |

---

## Table: `family_members`

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `name` | TEXT | Display name |
| `relationship` | TEXT | e.g. Spouse, Child |
| `phone` | TEXT NULL | |
| `email` | TEXT NULL | |
| `notes` | TEXT NULL | |
| `avatarPath` | TEXT NULL | Local encrypted path |
| `createdAt` / `updatedAt` | INTEGER | Epoch millis |

---

## Table: `emergency_contacts`

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `name` | TEXT | |
| `phone` | TEXT | |
| `alternatePhone` | TEXT NULL | |
| `email` | TEXT NULL | Added v8 |
| `relationship` | TEXT NULL | |
| `notes` | TEXT NULL | |
| `isPrimary` | INTEGER | Boolean |
| `linkedFamilyMemberId` | TEXT NULL | Added v7 |
| `createdAt` / `updatedAt` | INTEGER | Epoch millis |

---

## Table: `pets`

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `name` | TEXT | |
| `species` | TEXT NULL | |
| `breed` | TEXT NULL | |
| `birthDate` | INTEGER NULL | |
| `notes` | TEXT NULL | |
| `createdAt` / `updatedAt` | INTEGER | |

---

## Table: `pet_records`

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `petId` | TEXT FK | Parent pet |
| `title` | TEXT | |
| `recordType` | TEXT | e.g. vaccination, health |
| `notes` | TEXT NULL | |
| `recordDate` | INTEGER NULL | |
| `createdAt` / `updatedAt` | INTEGER | |

---

## Table: `medicines`

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `name` | TEXT | |
| `dosage` | TEXT NULL | |
| `familyMemberId` | TEXT NULL | |
| `petId` | TEXT NULL | |
| `scheduleNotes` | TEXT NULL | |
| `createdAt` / `updatedAt` | INTEGER | |

---

## Table: `reminders`

Unified reminder engine (v5+). Multiple offsets per record are separate rows sharing a `sourceKey` prefix.

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `title` | TEXT | |
| `linkedRecordId` | TEXT NULL | Vault record |
| `linkedPetId` | TEXT NULL | Pet profile |
| `linkedFamilyMemberId` | TEXT NULL | Family member |
| `linkedPetRecordId` | TEXT NULL | Pet vaccination / health record |
| `linkedMedicineId` | TEXT NULL | Medicine schedule |
| `category` | TEXT | e.g. `CUSTOM`, expiry-driven categories |
| `reminderDate` | INTEGER | Due date (epoch millis) |
| `reminderTimeMinutes` | INTEGER | Minutes from midnight (default 540 = 9:00 AM) |
| `triggerAt` | INTEGER | INDEX — alarm trigger timestamp |
| `repeatType` | TEXT | `NO_REPEAT`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `importance` | TEXT | `NORMAL`, `HIGH`, … |
| `status` | TEXT | INDEX — `ACTIVE`, `COMPLETED`, `SNOOZED` |
| `sourceKey` | TEXT | UNIQUE — dedupe key for engine sync |
| `notes` | TEXT NULL | |
| `createdAt` | INTEGER | |
| `actionedAt` | INTEGER NULL | When user marked actioned (v9) |

---

## Table: `backup_metadata`

Local encrypted backup file registry.

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `fileName` | TEXT | User-visible backup name |
| `localPath` | TEXT | App-private path |
| `fileSize` | INTEGER | Bytes |
| `recordCount` | INTEGER | Records included |
| `checksumSha256` | TEXT NULL | Integrity check |
| `appVersion` | TEXT NULL | Source app version |
| `createdAt` | INTEGER | INDEX |

---

## Table: `search_index`

Metadata-only search (never stores decrypted file content).

| Column | Type | Description |
|--------|------|-------------|
| `recordId` | TEXT PK | Vault record id |
| `title` | TEXT | INDEX |
| `category` | TEXT | INDEX |
| `tags` | TEXT NULL | Serialized tags |
| `updatedAt` | INTEGER | |

---

## Table: `activity_events`

Local-only Activity Insights event table added in v12. Stored in the encrypted SQLCipher database.

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `type` | TEXT, INDEX | `APP_OPENED`, `SESSION_ENDED`, `SCREEN_VIEW`, etc. |
| `timestamp` | INTEGER, INDEX | Epoch millis |
| `durationMs` | INTEGER NULL | Session duration for `SESSION_ENDED` events |
| `screen` | TEXT NULL, INDEX | Route id only; no screen content |
| `category` | TEXT NULL, INDEX | Category id only |
| `metadata` | TEXT NULL | Reserved for future non-sensitive aggregates |

Activity events must never store document titles, notes, file names, search query text, decrypted metadata, or attachment content.

---

## Type Converters

`RoomConverters` — `List<String>` ↔ delimited TEXT for `vault_records.tags`.

---

## Migrations

**Production:** explicit `Migration` objects in `DocuFindMigrations.kt`. **No** `fallbackToDestructiveMigration()`.

Every version bump is tested in `DocuFindMigrationTest`. Pre-migration backup via `DatabaseMigrationBackup`.

See [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md) and [DOCUFIND_DATA_SAFETY.md](./DOCUFIND_DATA_SAFETY.md).

### Version history

| Version | Changes |
|---------|---------|
| 1 | `documents`, `reminders` |
| 2 | Added `search_index` |
| 3 | `vault_records` + `vault_files`; family, emergency, pets, medicines, backup; migrated from `documents` |
| 4 | Family DOB/blood group; emergency alt phone; pet vaccination fields; reminders + `petRecordId` |
| 5 | Reminders unified engine (linked IDs, `sourceKey`, `triggerAt`, `status`) |
| 6 | No-op schema bump |
| 7 | Emergency `linkedFamilyMemberId` |
| 8 | Emergency `email` |
| 9 | `categoryMetadataJson`, `actionedAt` |

---

## DataStore (Non-Room)

Preferences in `docufind_preferences` — onboarding, user profile, PIN flags, vault lock, tagline preference, How to Use state. Not stored in Room. Safe across DB migrations.

See [DOCUFIND_DATA_SAFETY.md](./DOCUFIND_DATA_SAFETY.md).
