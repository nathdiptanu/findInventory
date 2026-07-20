# DocuFind — Testing Strategy

**Owner:** QA Architect / Release Quality  
**Status:** Active — required for all feature and bug-fix work

---

## Philosophy

DocuFind is a **production privacy app**. Tests exist to protect:

- User data (Room migrations, backup integrity)
- Security (PIN, vault gate, sensitive metadata)
- Core workflows (documents, reminders, search, family/emergency)
- Navigation and Compose UI on critical paths

**No feature is complete without updated tests.** See [Feature change rule](#feature-change-rule) below.

---

## Test layers

| Layer | Location | Runs on | Purpose |
|-------|----------|---------|---------|
| **Unit** | `app/src/test/java/.../unit/` | JVM | Pure logic, ViewModels with mocks, repository logic |
| **Repository** | `app/src/test/java/.../repository/` | JVM | Repository contracts with mocked DAOs |
| **Migration (JVM)** | `app/src/test/java/.../migration/` | JVM | Migration chain + SQL verification |
| **Database** | `app/src/androidTest/java/.../database/` | Device/emulator | In-memory Room DAO CRUD |
| **Migration (instrumented)** | `app/src/androidTest/.../migration/` | Device/emulator | Full schema round-trip via `MigrationTestHelper` |
| **UI (Compose)** | `app/src/androidTest/java/.../ui/` | Device/emulator | Critical composables and screens |
| **Integration** | `app/src/androidTest/java/.../integration/` | Device/emulator | Multi-module flows (expand as needed) |
| **Security** | `app/src/test/.../unit/security/` + androidTest | JVM + device | PIN, vault gate, metadata masking |

Shared fixtures: `app/src/test/java/com/docufind/app/testutil/TestFixtures.kt` (also on androidTest classpath).

---

## Folder structure

```
app/src/test/java/com/docufind/app/
├── testutil/           # Shared fixtures
├── unit/
│   ├── domain/       # CategoryFieldRegistry, HomeTaglines, RecordMetadata
│   ├── reminders/    # ReminderEngine, ReminderScheduleDefaults, TriggerCalculator
│   ├── security/     # PinManager, MainNavigationViewModel vault gate
│   └── validation/   # (legacy) FormValidation in ui/util — migrate over time
├── repository/       # SearchRepositoryImpl, etc.
├── migration/        # DocuFindMigrationTest (JVM)
├── reminder/         # Legacy ReminderTriggerCalculatorTest
├── ocr/              # MlKitOcrEngineTest, OcrInputValidatorTest, SearchIndexPolicyTest, etc.
├── export/pdf/       # EncryptedPdfWriterTest, PdfExportPasswordValidatorTest, etc.
├── ui/screens/add/   # OcrAcceptedTextStorageTest
├── ui/screens/reminders/ # RemindersUiStateTest
├── unit/scan/        # QrContentParserTest
├── security/crypto/  # Crypto unit tests
└── data/local/storage/ # VaultFileValidatorTest

app/src/androidTest/java/com/docufind/app/
├── database/         # VaultRecordDaoTest, ReminderDaoTest
├── ui/               # HowToUseSectionTest, future screen tests
├── integration/      # End-to-end flows (planned)
├── navigation/       # Nav graph tests (planned)
├── security/         # Instrumented security (planned)
└── data/local/db/migration/  # DocuFindMigrationInstrumentedTest
```

---

## Libraries

| Library | Use |
|---------|-----|
| JUnit 4 | All tests |
| MockK | Mocking DAOs, ciphers, auth |
| Kotlin Coroutines Test | `runTest` for suspend code |
| Turbine | Flow assertions (available; use for ViewModel Flow tests) |
| Google Truth | Readable assertions |
| Room Testing | `MigrationTestHelper`, in-memory DB |
| AndroidX Test | Instrumentation, Compose UI Test |
| Robolectric | Available; prefer JVM mocks + instrumented DB for Room |
| Hilt Android Testing | Available for future `@HiltAndroidTest` integration tests |

---

## What must be tested

### Always (every schema change)

- New `Migration(N, N+1)` in `DocuFindMigrations.kt`
- JVM migration test + instrumented migration test
- No `fallbackToDestructiveMigration()`

### Always (security changes)

- PIN verify/set paths
- Vault unlock gate before record/search navigation
- Sensitive metadata masked when `revealSensitive = false`
- `FLAG_SECURE` remains active during protected-to-protected navigation and is only cleared when no protected screen owns the activity window.
- Home Recent Items must not expose record title, file name, note, or timestamp before vault unlock.

### Always (reminder changes)

- Offset schedule (15, 7, 3, 2, 1, 0 days using the user-selected default notification time)
- Actioned → entire linked group completed
- Expiry sync creates/disables correct `sourceKey` rows
- Metadata-date sources create independent source-key families: PUC, vehicle insurance, warranty, refill, follow-up, vaccination, pet vaccination
- Notification actions remain Open, Mark Done, Snooze and do not crash if the reminder row is already gone
- Custom and overdue reminder edit must allow date/time reschedule through pickers, reject past triggers, cancel stale alarms, and schedule only the future trigger.
- Add-record save must ignore duplicate rapid submissions while a save is in progress.

### Always (UI/navigation changes)

- Compose UI test or integration test for changed screen
- Vault-gated routes still require unlock

### Always (OCR / export / QR changes)

- OCR accepted text must not enter search index (`SearchIndexPolicyTest`, `OcrAcceptedTextStorageTest`)
- PDF export password validation and sensitive field exclusion (`PdfExportPasswordValidatorTest`, `PdfExportContentFilterTest`)
- `DOCUMENT_EXPORT` re-auth when vault unlocked (`AuthGateTest`)
- QR parsing for phone/email/URL safety (`QrContentParserTest`)
- Upcoming reminder tab filter (`RemindersUiStateTest`)

### Always (activity insights changes)

- Verify no network permission, SDK, endpoint, advertising ID, or telemetry upload is introduced.
- Verify events never include document titles, file names, notes, search query text, decrypted metadata, or attachment content.
- Verify v11 -> v12 migration creates `activity_events` without touching Vault, file, reminder, family, pet, backup, or search rows.
- Smoke-test Settings -> Activity Insights for Daily, Weekly, and Monthly filters on a small-screen device.
- Confirm app open/session duration, search submit, vault unlock, document save, reminder creation, and Mark Done update local counts without crashing.

---

## Commands

### Fast validation (after small changes)

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind

# Compile Kotlin (app + unit test sources)
.\gradlew.bat compileDebugKotlin

# Fast validation (after small changes)
.\gradlew.bat fastValidation
# equivalent: testDebugUnitTest

# Phase completion
.\gradlew.bat fullValidation
# assembleDebug + testDebugUnitTest + lintDebug

# Individual gates
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

### Instrumented (device/emulator required)

```powershell
.\gradle-dist\gradle-8.9\bin\gradle.bat connectedDebugAndroidTest
```

### Before APK / release candidate

```powershell
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug
.\gradle-dist\gradle-8.9\bin\gradle.bat testDebugUnitTest
.\gradle-dist\gradle-8.9\bin\gradle.bat connectedDebugAndroidTest
.\gradle-dist\gradle-8.9\bin\gradle.bat lintDebug
```

Optional clean (Windows may lock lint cache):

```powershell
.\gradle-dist\gradle-8.9\bin\gradle.bat clean
```

---

## Coverage targets

| Area | Target | Notes |
|------|--------|-------|
| Pure business logic | **~100%** | Validators, calculators, registries |
| Repositories / ViewModels | **90%+** | Focus on risk paths, not getters |
| Migrations | **100% chain** | Every version step covered |
| Critical UI flows | **Must have tests** | Even if total app coverage is lower |

Do **not** inflate coverage with trivial tests.

---

## CI-ready checklist

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` passes (0 errors)
- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew connectedDebugAndroidTest` passes on CI device farm or local device
- [ ] No `fallbackToDestructiveMigration` in codebase
- [ ] Migration tests updated if `SCHEMA_VERSION` changed
- [ ] Logcat: no FATAL on smoke flows

---

## Critical regression flows (manual + automated)

| Flow | Automated coverage |
|------|-------------------|
| Recent item → vault unlock | `MainNavigationViewModelVaultGateTest` |
| Search empty DB | `SearchRepositoryImplTest` |
| Reminder 15/7/3/2/1/0 offsets | `ReminderScheduleDefaultsTest`, `ReminderEngineTest` |
| Mark actioned stops group | `ReminderEngineTest` |
| Upcoming tab filter | `RemindersUiStateTest` |
| OCR not in search index | `SearchIndexPolicyTest`, `OcrAcceptedTextStorageTest` |
| PDF export password/filter | `PdfExportPasswordValidatorTest`, `PdfExportContentFilterTest` |
| QR content parsing | `QrContentParserTest` |
| Export re-auth | `AuthGateTest` |
| Banking/property registries | `CategoryFieldRegistryTest` |
| Multiple files per record | `VaultRecordDaoTest` |
| Category sensitive fields | `RecordMetadataTest`, `CategoryFieldRegistryTest` |
| PIN validation | `PinManagerTest` |
| Room migration preserves data | `DocuFindMigrationTest`, `DocuFindMigrationInstrumentedTest` |
| How to Use expand/collapse | `HowToUseSectionTest` |

---

## Security Hardening Verification - 2026-07-03

- `DocuFindMigrationTest.migration10To11_sanitizesSearchIndexOnly` verifies the v10 -> v11 migration clears derived search cache fields without touching Vault tables.
- `DocuFindMigrations.ALL` is expected to cover every version from 1 through `DocuFindDatabase.SCHEMA_VERSION`.
- Phase build gate: `./gradlew.bat assembleDebug` PASS.
- No APK/AAB handoff is produced in this phase.

Recommended manual QA:

- Upgrade from a v10 install with records, attachments, reminders, family, pets, and backup settings.
- Confirm Vault records and files remain available after upgrade.
- Confirm search still finds records by title/category/user tags but not by encrypted ID/card/policy/password fields.
- Confirm screenshots are blocked on Security Settings and all protected Vault surfaces.

---

## Activity Insights Verification - 2026-07-03

- Phase build gate: `./gradlew.bat assembleDebug` PASS.
- Verify Settings contains Activity Insights.
- Verify the screen shows "All activity insights stay on your device."
- Verify no internet/server analytics dependency was added.

---

## Security and Reminder Hardening Verification - 2026-07-17

- Phase build gate: `./gradlew.bat assembleDebug` PASS.
- Manual QA required on device: navigate Vault -> record detail -> preview and confirm screenshots/recents preview remain blocked through transitions.
- Manual QA required on Home: Recent Items must show protected labels, not record names or timestamps.
- Manual QA required on Reminders: edit an overdue reminder to a future date/time, confirm only one future notification is scheduled, and Mark Done stops the linked offset series.
- JVM unit and lint gates should still be run before APK handoff; this workspace has previously required missing offline test dependencies to be restored before those checks can pass.
- Verify Room schema exports version 12 after build.
- No APK/AAB handoff is produced in this phase.

---

## Production Readiness Verification - 2026-07-20

Build gates (run from repo root):

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest      # fastValidation
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat fullValidation         # assemble + unit + lint
```

Manual QA: see [DOCUFIND_PRODUCTION_READINESS.md](./DOCUFIND_PRODUCTION_READINESS.md#13-manual-regression-checklist) and [DOCUFIND_FINAL_QA.md](./DOCUFIND_FINAL_QA.md#production-readiness-pass---2026-07-20).

New unit test packages: `ocr`, `export/pdf`, `unit/scan`, expanded `unit/security`, `ui/screens/add`, `ui/screens/reminders`.

No Room schema bump for Banking/Property fields (still v12).

---

## Migration testing rules

1. Bump `DocuFindDatabase` version and add `MIGRATION_N_N+1`.
2. Export schema JSON (`app/schemas/.../N.json`) via KSP build.
3. Add JVM test asserting SQL / chain integrity.
4. Add instrumented test seeding old schema + data → migrate → assert rows.
5. Never use destructive fallback.
6. See [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md).

---

## Feature change rule

For **every** future feature or bug fix:

1. Update or add relevant **unit tests**.
2. Update or add **integration/UI tests** if navigation or screen behavior changes.
3. Update **migration tests** if database schema changes.
4. Run **`fastValidation`** (`testDebugUnitTest`).
5. Run **`fullValidation`** before APK / phase sign-off.
6. Update **docs** (`DOCUFIND_TEST_COVERAGE_REPORT.md`, release notes if shipping).

**No feature is complete unless tests are updated and passing.**

---

## Guidance for Cursor / Codex agents

When modifying DocuFind:

1. Read this file and [DOCUFIND_INTEGRATION_INTEGRITY.md](./DOCUFIND_INTEGRATION_INTEGRITY.md).
2. Identify affected layer (unit / DB / UI / migration).
3. Add or extend tests in the matching package **before** marking task done.
4. Run `fastValidation` minimum; `fullValidation` before release tasks.
5. Update `DOCUFIND_TEST_COVERAGE_REPORT.md` with new tests and any remaining gaps.

See also: [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md), [DOCUFIND_DATA_SAFETY.md](./DOCUFIND_DATA_SAFETY.md)

---

## Production Readiness Verification - 2026-07-20

Build gates (run from repo root):

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest      # fastValidation
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat fullValidation         # assemble + unit + lint
```

Manual QA: see [DOCUFIND_PRODUCTION_READINESS.md](./DOCUFIND_PRODUCTION_READINESS.md#13-manual-regression-checklist) and [DOCUFIND_FINAL_QA.md](./DOCUFIND_FINAL_QA.md#production-readiness-pass---2026-07-20).

New unit test packages: `ocr`, `export/pdf`, `unit/scan`, expanded `unit/security`, `ui/screens/add`, `ui/screens/reminders`.

No Room schema bump for Banking/Property fields (still v12).
