# DocuFind — Architecture

## Overview

DocuFind uses **Clean Architecture–inspired layering** with **MVVM** on the UI side. All data stays on-device.

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose + Navigation Compose)      │
│  Screens · ViewModels · Theme · Components              │
├─────────────────────────────────────────────────────────┤
│  Domain Layer                                           │
│  Models · Repository interfaces                         │
├─────────────────────────────────────────────────────────┤
│  Data Layer                                             │
│  Repository impls · Room · DataStore · Keystore         │
└─────────────────────────────────────────────────────────┘
```

## Module Structure (Single `:app` Module)

Foundation uses a single app module with package-based layers:

```
com.docufind.app
├── di/                 # Hilt modules
├── data/
│   ├── local/
│   │   ├── db/         # Room entities, DAOs, database
│   │   ├── datastore/  # Preferences DataStore
│   │   └── keystore/   # Android Keystore manager
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models & enums
│   └── repository/     # Repository contracts
└── ui/
    ├── theme/          # Material 3 theme
    ├── navigation/     # Routes, main scaffold
    ├── components/     # Shared Compose components
    └── screens/        # Feature screens + ViewModels
```

## MVVM Flow

1. **Compose Screen** observes `StateFlow` / `Flow` from ViewModel
2. **ViewModel** calls repository interfaces (never Room/DataStore directly)
3. **Repository** maps entities ↔ domain models and coordinates local stores
4. **Hilt** wires dependencies at compile time

Example (Home categories):

```
HomeScreen → HomeViewModel → DocumentRepository → DocumentDao → Room
```

## Dependency Injection (Hilt)

| Module | Provides |
|--------|----------|
| `DatabaseModule` | `DocuFindDatabase`, DAOs |
| `RepositoryModule` | Binds repository interfaces to impls |
| `OcrModule` | `OcrEngine`, `TextRecognizerProvider`, `OcrTempStore` |

`@HiltViewModel` ViewModels receive repositories via constructor injection.

## Navigation

Two-level navigation:

1. **Root NavHost** (`DocuFindApp`): Splash → Onboarding → PIN → Biometric → Main
2. **Main NavHost** (`MainScreen`): Bottom tabs (Home, Vault, Reminders, Settings)

Route constants live in `DocuFindRoutes`.

## State Management

- **UI state:** `StateFlow` in ViewModels, collected with `collectAsStateWithLifecycle`
- **Preferences:** DataStore `Flow<AppPreferences>`
- **Database:** Room DAOs expose `Flow` for reactive lists/counts

## Vault stability states

Vault and protected document screens must expose explicit UI states instead of relying on blank content while flows load.

| Area | States |
|------|--------|
| Vault tab | Setup, Locked, Opening animation, Content |
| Module lists | Loading, Empty, Error with Retry, Content |
| Record detail | Loading, Not found, Error snackbar, Content |
| File actions | Auth required, Ready, Error |

After PIN or biometric unlock, `VaultTabScreen` shows a lightweight shield/document opening animation while the unlocked content is already being prepared behind it. The animation is visual-only and must not block data loading or database work.

Protected record navigation continues through `MainNavigationViewModel.openRecord()` and record detail still performs a defense-in-depth lock check before collecting record data.

## Concurrency

- Kotlin Coroutines + `viewModelScope`
- Room and DataStore are main-safe via their APIs
- No global singletons outside DI

## Technology Stack

| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + repository pattern |
| DI | Hilt (KSP) |
| DB | Room (KSP) |
| Preferences | DataStore Preferences |
| Security keys | Android Keystore |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 35 |
| Camera / scan | CameraX 1.4.1 — QR scanner live preview |
| OCR / barcode | ML Kit text recognition + barcode scanning |
| PDF export | PdfBox-Android 2.0.27.0 (Apache-2.0) |

## Future Layer Additions

- `data/local/files/` — encrypted file I/O
- `domain/usecase/` — use cases when logic grows
- `ui/screens/document/` — detail, viewer, import
- WorkManager for local backup jobs (no cloud)

## Testing Strategy (Planned)

- Unit tests: ViewModels, repositories (fake DAOs)
- Instrumentation: navigation smoke, Room migrations
- No network mocks required (offline-first)

## Reminder Architecture Update - 2026-07-02

`ReminderEngine` is the single coordination point for linked reminders. Vault records can now sync multiple source-key families from both top-level expiry dates and category metadata dates, including vehicle insurance, PUC, warranty, prescription refill/follow-up, vaccination next due, and pet vaccination.

Default reminder time is stored in DataStore preferences and surfaced on the Reminders screen. Auto-generated expiry schedules use that local-time setting when calculating 15/7/3/2/1/due-date alarms.

Notification flow remains local-only: `AlarmManager` -> `ReminderAlarmReceiver` -> `ReminderNotificationHelper`. Android 13+ notification permission is checked before posting. Notification actions route through `ReminderActionReceiver` for Open, Mark Done, and Snooze.

## Universal File Preview Architecture Update - 2026-07-02

Vault uploads are copied into app-private encrypted storage through `VaultFileImporter` and `EncryptedFileManager`; the UI no longer depends on external picker URIs after save. `FilePreviewViewModel` loads the `VaultFile`, related `VaultRecord` category, and decrypted preview file through `SecureFileAccessManager`.

The preview state is explicit: loading, image, PDF, unsupported/unavailable, missing file, error, and deleted. Missing encrypted files surface a user-safe message instead of throwing into the UI. Single-attachment deletion is routed through `VaultRecordRepository.deleteFile()` and `VaultRecordSecureDelete.deleteFile()` so the encrypted file, key material, thumbnail, cache entry, and database row are removed together.

## Security Hardening Architecture Update - 2026-07-03

Database version is now 11. The v10 -> v11 migration is non-destructive and sanitizes only the derived `search_index` cache. It clears cached tags, notes, file names, and rebuilds `searchText` from non-sensitive columns. Real Vault records, encrypted files, reminders, family records, pet records, and backup data are untouched.

New search index writes now use `RecordMetadata.userTags(record.tags)` so encrypted `meta:*` category metadata is not mirrored into the searchable cache. Record detail rendering now uses the encrypted-aware `RecordMetadata` path only, so sensitive fields remain masked until the Vault session is unlocked.

Screenshot blocking is explicitly enabled on Security Settings in addition to Vault, Add record, record detail, preview, search, backup, family, pets, emergency, PIN setup, and unlock overlays.

## Activity Insights Architecture Update - 2026-07-03

Database version is now 12. The v11 -> v12 migration adds `activity_events`, an encrypted SQLCipher-backed local-only table for coarse activity facts. The table stores event type, timestamp, optional screen/category, and optional session duration only.

`ActivityInsightsTracker` records app foreground/background sessions, screen route visits, vault opens, search submissions, document saves, reminder creation, and reminder completion. It uses an internal IO coroutine scope and catches write failures so insights tracking can never block or crash a user workflow.

`ActivityInsightsRepository` computes reports from local sources:

- `activity_events` for app opens, session duration, screen visits, vault opens, and search count.
- `vault_records` for documents added trends and category-wise counts.
- `vault_files` for files stored and attachment storage usage.
- `reminders` for created/completed/active/disabled reminder summaries.

No server, SDK, network transport, advertising identifier, or telemetry upload is involved.

## Production Readiness Architecture Update - 2026-07-20

New feature packages (still single `:app` module):

```
com.docufind.app/
├── ocr/                 MlKitOcrEngine, OcrTempStore, PdfOcrHelper, OcrModule
├── export/pdf/          PdfExportManager, WatermarkedPdfBuilder, EncryptedPdfWriter
├── scan/                QrContentParser, SafeUrlValidator, PhoneNumberNormalizer
└── ui/screens/scan/     QrScannerScreen (CameraX + ML Kit barcode)
```

**OCR flow:** Add Document attachment → temp copy in `cache/ocr/` → ML Kit recognition → review sheet → accepted text merged into record notes (`OcrAcceptedTextStorage`) → temp wiped. `SearchIndexPolicy` keeps OCR text out of `search_index`. `DocuFindApplication` wipes `OcrTempStore` on vault lock alongside `SecureFileCache`.

**PDF export flow:** `AuthGate` (DOCUMENT_EXPORT, always re-auth) → collect metadata/attachments → watermarked plain PDF → `StandardProtectionPolicy` encrypt → share/save → secure delete temps.

**QR scan flow:** CameraX preview + ML Kit analysis → `QrContentParser` → confirmation sheet → safe external intents (`ACTION_DIAL` for phone, http/https only for URLs).

**Reminders:** `ReminderDateTimeField` for custom create/edit; `ReminderBootReceiver` listens for `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`.

Room schema unchanged at **v12** for Banking/Property registry fields.

See [DOCUFIND_PRODUCTION_READINESS.md](./DOCUFIND_PRODUCTION_READINESS.md).
