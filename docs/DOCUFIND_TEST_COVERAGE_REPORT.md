# DocuFind — Test Coverage Report

**Last updated:** 2026-07-20  
**Schema version:** 12

---

## Summary

| Metric | Value |
|--------|-------|
| JVM unit test classes | **32+** (includes production-readiness pass) |
| Instrumented test classes | **4** |
| Gradle fast gate | `fastValidation` → `testDebugUnitTest` |
| Gradle full gate | `fullValidation` → `assembleDebug` + `testDebugUnitTest` + `lintDebug` |
| androidTest compile | `assembleDebugAndroidTest` |
| Instrumented run | `connectedDebugAndroidTest` — separate device step |

Run: `.\gradlew.bat testDebugUnitTest`

---

## Covered modules

### Unit — domain & validation

| Module | Test class | Notes |
|--------|------------|-------|
| `CategoryFieldRegistry` | `unit/domain/CategoryFieldRegistryTest` | Banking, property, cards, insurance rules |
| `HomeTaglines` | `unit/domain/HomeTaglinesTest` | Pool size, pickRandom |
| `RecordMetadata` | `unit/domain/RecordMetadataTest` | Encrypt + mask sensitive |
| `FormValidation` | `ui/util/FormValidationTest` | Name/phone/email (legacy path) |

### Unit — reminders

| Module | Test class | Notes |
|--------|------------|-------|
| `ReminderScheduleDefaults` | `unit/reminders/ReminderScheduleDefaultsTest` | 15/7/3/1/0 offsets |
| `ReminderTriggerCalculator` | `reminder/ReminderTriggerCalculatorTest` | Date/time math |
| `ReminderEngine` | `unit/reminders/ReminderEngineTest` | Sync, actioned group, upsert |

### Unit — security

| Module | Test class | Notes |
|--------|------------|-------|
| `PinManager` | `unit/security/PinManagerTest` | Hash verify, no plaintext storage |
| `MainNavigationViewModel` | `unit/security/MainNavigationViewModelVaultGateTest` | Record/search vault gate |
| Crypto stack | `security/crypto/*Test` | AES-GCM, PBKDF2, secure memory |
| Backup crypto | `security/backup/BackupEncryptionTest` | Encrypt/validate backup |

### Unit — storage & files

| Module | Test class | Notes |
|--------|------------|-------|
| `VaultFileValidator` | `data/local/storage/VaultFileValidatorTest` | MIME + size limits |

### Unit — repository

| Module | Test class | Notes |
|--------|------------|-------|
| `SearchRepositoryImpl` | `repository/SearchRepositoryImplTest` | Empty DB search safe |

### Unit — OCR (2026-07-20)

| Module | Test class | Notes |
|--------|------------|-------|
| `OcrInputValidator` | `ocr/OcrInputValidatorTest` | MIME + size limits |
| `OcrResultMapper` | `ocr/OcrResultMapperTest` | Progress messages |
| `SearchIndexPolicy` | `ocr/SearchIndexPolicyTest` | OCR excluded from index |
| `MlKitOcrEngine` | `ocr/MlKitOcrEngineTest` | Engine wiring |
| `PdfOcrHelper` | `ocr/PdfOcrHelperTest` | PDF page raster path |
| `OcrAcceptedTextStorage` | `ui/screens/add/OcrAcceptedTextStorageTest` | Notes-only merge |

### Unit — PDF export (2026-07-20)

| Module | Test class | Notes |
|--------|------------|-------|
| `PdfExportPasswordValidator` | `export/pdf/PdfExportPasswordValidatorTest` | Min 8 chars |
| `PdfExportContentFilter` | `export/pdf/PdfExportContentFilterTest` | Password fields excluded |
| `PdfExportFilenameSanitizer` | `export/pdf/PdfExportFilenameSanitizerTest` | Safe filenames |
| `EncryptedPdfWriter` | `export/pdf/EncryptedPdfWriterTest` | StandardProtectionPolicy |

### Unit — QR scan (2026-07-20)

| Module | Test class | Notes |
|--------|------------|-------|
| `QrContentParser` | `unit/scan/QrContentParserTest` | tel/mailto/vCard/URL/plain |

### Unit — auth + reminders UI (2026-07-20)

| Module | Test class | Notes |
|--------|------------|-------|
| `AuthGate` | `unit/security/AuthGateTest` | DOCUMENT_EXPORT always re-auth |
| `RemindersUiState` | `ui/screens/reminders/RemindersUiStateTest` | Upcoming tab filter |
| `ReminderRepositoryImpl` | `unit/reminders/ReminderRepositoryImplTest` | Custom reminder validation |

### Unit — migration (JVM)

| Module | Test class | Notes |
|--------|------------|-------|
| `DocuFindMigrations` | `migration/DocuFindMigrationTest` | Chain + SQL for v1→9 |

### Instrumented — database

| Module | Test class | Notes |
|--------|------------|-------|
| `VaultRecordDao` / `VaultFileDao` | `database/VaultRecordDaoTest` | CRUD, recent, multi-file |
| `ReminderDao` | `database/ReminderDaoTest` | Insert, actioned, delete |

### Instrumented — migration

| Module | Test class | Notes |
|--------|------------|-------|
| Full schema path | `DocuFindMigrationInstrumentedTest` | v1→9, data preservation samples |

### Instrumented — UI

| Module | Test class | Notes |
|--------|------------|-------|
| `HowToUseSection` | `ui/HowToUseSectionTest` | Expand/collapse toggle |

---

## Uncovered modules (known gaps)

| Area | Risk | Priority |
|------|------|----------|
| `AddDocumentViewModel` | Save flow, multi-attachment, OCR cancel | **High** |
| `SearchViewModel` | Empty query UI state | Medium |
| `HomeViewModel` | Tagline + welcome binding | Medium |
| `RemindersViewModel` | Edit/delete UI | Medium |
| `VaultViewModel` | Biometric fallback | Medium |
| `PreferencesRepository` / onboarding | Profile save flow | Medium |
| `ReminderNotificationHelper` | Post permission / channel | Medium (instrumented) |
| `BackupRepository` / restore | End-to-end restore | High |
| `EmergencyContact` / `FamilyMember` repositories | Full CRUD | Medium |
| Full nav graph Compose tests | Splash → main | High |
| Settings Security/Storage screens | Open without crash | Medium |
| `ScreenshotProtection` FLAG_SECURE | Instrumented window flags | Low |
| Hilt `@AndroidEntryPoint` receivers | Boot/alarm receivers | Low |

---

## Next test priorities

1. `AddDocumentViewModelTest` — category fields + multiple attachments mock repository
2. `HomeViewModelTest` — tagline rotation + username (Turbine + mock prefs)
3. `androidTest/integration/DocumentReminderIntegrationTest` — save record → 5 reminders in DB
4. `androidTest/ui/SettingsScreensSmokeTest` — Security + Storage compose smoke
5. `androidTest/navigation/VaultGateNavigationTest` — full unlock overlay flow
6. `BackupValidator` integration with sample payload
7. Expand `DocuFindMigrationInstrumentedTest` — sensitive tags preserved across upgrade

---

## Skipped tests and why

| Item | Reason |
|------|--------|
| Robolectric migration round-trip on JVM | SSL/SDK fetch issues on some dev machines; use instrumented migration tests instead |
| Full `@HiltAndroidTest` app launch | Planned; requires test application module setup |
| `connectedDebugAndroidTest` in CI | Requires connected device/emulator — not run in default `fullValidation` |

---

## Commands last run

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest          # fastValidation
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat fullValidation             # assemble + unit + lint
.\gradlew.bat assembleDebugAndroidTest   # instrumented test APK
# .\gradlew.bat connectedDebugAndroidTest  # requires adb + device/emulator
```

**Last validated:** 2026-07-20 (documentation pass; run gates locally before release)

---

## Feature change rule

See [DOCUFIND_TESTING_STRATEGY.md](./DOCUFIND_TESTING_STRATEGY.md#feature-change-rule).
