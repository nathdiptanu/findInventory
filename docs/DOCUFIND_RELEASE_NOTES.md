# DocuFind — Release Notes

## Production readiness pass - 2026-07-20

Build gate: `./gradlew.bat fullValidation` (`assembleDebug` + `testDebugUnitTest` + `lintDebug`).

- **Launcher icon:** Adaptive foreground and monochrome layers use uniform **0.90** scale centred at `(54, 54)`; `ic_docufind_logo_reference.xml` vector restored for in-app splash/brand mark.
- **Reminders:** `ReminderDateTimeField` combines date + Material time picker; `ReminderAlarmScheduler` uses exact alarms with inexact fallback; exact-alarm banner on Reminders screen; `ReminderBootReceiver` reschedules on boot, `TIME_SET`, and `TIMEZONE_CHANGED`.
- **Upcoming tab:** Active reminders with future `triggerAt`, sorted chronologically.
- **OCR:** ML Kit text recognition for JPEG/PNG/PDF on Add Document; review sheet; accepted text stored in notes only; temp cache wiped on vault lock; OCR text excluded from search index.
- **Secure PDF export:** PdfBox watermarked build + `StandardProtectionPolicy` encryption; password min 8 characters; sensitive/password fields excluded from export body.
- **QR scanner:** CameraX live scan + gallery fallback; ML Kit barcodes; confirmation before external actions; phone uses `ACTION_DIAL`.
- **Auth:** `DOCUMENT_EXPORT` always requires fresh PIN/biometric even when vault session is unlocked.
- **Categories:** Banking (`finance`) and Property field registries on Quick Access; no Room schema bump (still v12).
- **Dependencies:** CameraX 1.4.1, ML Kit text/barcode, PdfBox-Android 2.0.27.0 (Apache-2.0) — expect APK size increase.
- **Tests:** New unit coverage under `ocr`, `export/pdf`, `unit/scan`, `AuthGateTest`, `RemindersUiStateTest`, `OcrAcceptedTextStorageTest`, expanded `CategoryFieldRegistryTest`.

See [DOCUFIND_PRODUCTION_READINESS.md](./DOCUFIND_PRODUCTION_READINESS.md) for architecture detail and manual regression checklist.

## Security and reminder hardening - 2026-07-17

Build gate: `./gradlew.bat assembleDebug` PASS.

- Added reference-counted secure-screen handling so protected Vault/document/preview screens do not accidentally clear `FLAG_SECURE` during secure-to-secure navigation.
- Home Recent Items now hide sensitive record titles and timestamps before unlock, replacing them with a protected Vault label and lock affordance.
- Custom reminder create/edit now uses a compact time row that opens a Material clock picker instead of manual time entry.
- Changing the default notification time now reschedules active linked expiry/renewal/refill/vaccination offset reminders to the new time.
- Reminder saves now reject blank titles and past trigger times; overdue reminders must be moved to a future trigger before they can be rescheduled.
- Add-record save flow now guards against rapid duplicate submissions while a save is already in progress.
- Launcher icon foreground was reduced inside the adaptive icon safe area so the D mark is less cramped on round/squircle OEM masks.
- Full 21-phase roadmap items such as OCR extraction, PDF export polish, and expanded instrumentation tests remain planned work unless separately implemented and verified.

## Launcher icon safe-zone fix - 2026-07-17

Build gate: `./gradlew.bat assembleDebug` PASS.

- Updated adaptive launcher icons to use the padded vector DocuFind D foreground instead of the full-canvas bitmap reference.
- Kept the launcher adaptive background white so OEM launcher masks render the mark as a blue D on white rather than a compressed blue circular icon.
- Tuned the foreground vector scale and centering so the keyhole remains visible across round, squircle, and rounded-square masks.
- No package name, signing configuration, app ID, database schema, or user-data path was changed.

## Local-only Activity Insights - 2026-07-03

Build gate: `./gradlew.bat assembleDebug` PASS.

- Added Settings -> Activity Insights with Daily, Weekly, and Monthly local reports.
- Added local-only tracking for app opens, session duration, screen visit counts, vault opens, search usage, documents added, reminders created/completed, files stored, and storage usage.
- Added lightweight report cards for activity trends, document additions, file storage trends, category-wise document counts, and reminder completion.
- Bumped Room schema to version 12 with a non-destructive `activity_events` migration.
- Activity events are stored only in the encrypted local SQLCipher database and do not include document titles, file names, notes, search query text, decrypted metadata, or attachment content.
- No APK/AAB handoff is produced in this phase.

## Security hardening and migration safety - 2026-07-03

Build gate: `./gradlew.bat assembleDebug` PASS.

- Bumped Room schema to version 11 with a non-destructive search-cache sanitation migration.
- New and migrated `search_index` rows no longer mirror encrypted metadata tags, notes, or file names.
- Record detail now renders category metadata only through the encrypted-aware, Vault-unlock-aware metadata path.
- Security Settings now blocks screenshots/recents preview via `FLAG_SECURE`.
- Added JVM migration coverage for the v10 -> v11 search-cache sanitation.
- No APK/AAB handoff is produced in this phase.

## Universal preview and attachment handling - 2026-07-02

Build gate: `./gradlew.bat assembleDebug` PASS.

- Added universal Vault attachment preview metadata: file name, size, type, created date, and category.
- Added preview actions for Back, Open, Share, Download, and Delete.
- Image previews remain inline; PDF previews fall back gracefully when rendering is unavailable.
- Missing encrypted files now show "File unavailable. The original file may have been moved or deleted." instead of crashing.
- New Vault uploads continue to be copied into app-private encrypted storage, with legacy/missing-file handling made safe.
- Pet profile photos now support preview, replace, remove, and safe gallery cancel behavior.
- No APK/AAB handoff is produced in this phase.

## Reminder time and expiry schedule fix - 2026-07-02

Build gate: `./gradlew.bat assembleDebug` PASS.

- Added a user-changeable default notification time on the Reminders screen.
- Auto-generated expiry/renewal/refill/vaccination schedules now use the selected local notification time.
- Default linked reminder offsets now include 15, 7, 3, 2, 1 days before, plus the due date.
- Mark Done resolves the whole linked reminder series for that expiry/refill/renewal event so remaining offsets do not continue firing.
- Vaccination add-record metadata now includes Date given and Next due date so expiry-based reminders have the correct source date.
- No APK/AAB handoff is produced in this phase.

## Vault category audit - 2026-07-02

Build gate: `./gradlew.bat assembleDebug` PASS.

- Category metadata registry now covers Documents, ID Cards, Cards, Medical, Prescriptions, Vaccination, Education, Insurance, Vehicle, Warranty, Pets, Family, Emergency, and Others.
- Primary date fields now use category-specific labels such as Card expiry, Warranty expiry, Renewal / expiry date, Next due date, and Refill / follow-up date.
- Reminder-enabled categories now default on for expiry/due-date categories including ID Cards, Cards, Prescriptions, Vaccination, Pets, Warranty, Vehicle, Insurance, and Documents.
- Family, Emergency, and Others no longer fall back to empty metadata forms.
- Module detail metadata definitions were aligned with the Add Document category field keys so saved values render consistently.
- No APK/AAB handoff is produced in this phase.

## Vault stability pass - 2026-07-01

Build gate: `./gradlew.bat assembleDebug` PASS.

- Vault tab now shows a clean shield/document opening animation after PIN or biometric unlock.
- Module category lists now expose loading, empty, error-with-retry, and content states.
- Record detail edit, delete, share, and download operations now catch repository/file exceptions and surface errors instead of crashing.
- File action UI was adjusted to avoid button crowding on small screens.
- No release APK/AAB handoff was produced in this phase.

## v1.0.0 — Debug release (2026-06-29)

**APK:** `app/build/outputs/apk/debug/app-debug.apk` (~41 MB)  
**Validated:** `assembleDebug test lintDebug` PASS; device install + cold start PASS (CPH2707)

### QA fixes in this build

- **Family profile camera crash** — FileProvider now includes `cache/profile_photos/` path (fixes FATAL when taking a family member photo with the camera).
- **Vault screen lint** — Scaffold content padding applied on unlock screen.

### Highlights

- Full first-run flow: splash animation → onboarding → profile setup → home.
- Home: personalized welcome, rotating taglines, accent first letter, Quick Access grid, How to Use card.
- Encrypted vault: PIN + biometric, screenshot protection on sensitive screens.
- Add document: multiple files, category-specific fields, date picker, encrypted sensitive metadata.
- Reminders: multi-offset expiry schedule (15/7/3/2/1/0 days at the user-selected notification time), edit/delete/actioned, test notification.
- Settings: Security, Storage, Help & Support (email with diagnostics).
- Family & emergency contacts with polished forms and keyboard-safe layout.
- Room schema v9 with explicit migrations — upgrades preserve user data (no destructive fallback).

### Technical QA

| Check | Result |
|-------|--------|
| `assembleDebug` | PASS |
| `test` | PASS (41 unit tests) |
| `lintDebug` | PASS |
| Device install + launch | PASS |
| No destructive migration | PASS |
| Secure logging | PASS |

### Documentation

- `DOCUFIND_FINAL_QA.md` — full regression matrix
- `DOCUFIND_RELEASE_CHECKLIST.md` — release gates
- `DOCUFIND_KNOWN_LIMITATIONS.md` — updated
- `DOCUFIND_MIGRATION_STRATEGY.md` / `DOCUFIND_DATA_SAFETY.md` — data safety

---

## Prior unreleased work (included in v1.0.0)

### Navigation & stability

- Vault tab: PIN setup, lock, unlocked browse.
- Search with empty DB, filters, back navigation.
- Recent items vault gate; invalid record IDs handled safely.
- All Settings sub-screens registered and tappable.

### Reminders & notifications

- Reliable notification channel, permission tracking, `ic_stat_docufind` icon.
- Notification actions: Mark actioned, Snooze, View record.
- Reschedule on app start and boot.

### Forms & polish

- `DocuFindFormScaffold` — keyboard gap fix (`adjustNothing` + `imePadding`).
- Emergency contact grouped form with email and relation picker.
- Database v8+ emergency email; v9 category metadata + reminder `actionedAt`.

### Launch crash fix

- `Theme.DocuFind` extends AppCompat (required for biometrics).

---

## Earlier phases

- Encrypted vault, modules, family/pets/emergency, backup/restore, search/preview/share, support email, onboarding/PIN/biometric.
