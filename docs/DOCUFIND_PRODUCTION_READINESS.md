# DocuFind — Production Readiness Pass (2026-07-20)

Summary of the production-readiness work completed in `app/src`. This pass adds OCR, secure PDF export, QR scanning, reminder hardening, Banking/Property category registries, launcher icon fixes, and expanded unit tests — without a Room schema bump.

**Related docs:** [DOCUFIND_RELEASE_NOTES.md](./DOCUFIND_RELEASE_NOTES.md) · [DOCUFIND_KNOWN_LIMITATIONS.md](./DOCUFIND_KNOWN_LIMITATIONS.md) · [DOCUFIND_FINAL_QA.md](./DOCUFIND_FINAL_QA.md)

---

## 1. Launcher icon and resource structure

| Asset | Path | Notes |
|-------|------|-------|
| Adaptive foreground | `res/drawable/ic_launcher_foreground.xml` | Brand D + keyhole paths centred on canvas `(54, 54)` with **uniform scale 0.90** (no independent X/Y stretch) |
| Monochrome layer | `res/drawable/ic_launcher_monochrome.xml` | Simplified D + keyhole silhouette; same **uniform 0.90** scale for themed icons |
| Adaptive wrappers | `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | White background + foreground + monochrome |
| In-app reference | `res/drawable/ic_docufind_logo_reference.xml` | Full vector D mark restored for splash / `DocuFindBrandMark` (`ContentScale.Fit`) |

The foreground vector keeps the mark inside the Android adaptive **66% safe zone** so round/squircle OEM masks do not crop or compress the D into a blue circle.

See [DOCUFIND_BRAND_GUIDELINES.md](./DOCUFIND_BRAND_GUIDELINES.md).

---

## 2. Reminder architecture, date/time picker, and alarms

### Components

| Piece | Role |
|-------|------|
| `ReminderDateTimeField` | Combined date (`DocuFindDateField`) + Material 3 `TimePicker` dialog; shows formatted trigger preview and past-time warning |
| `ReminderAlarmScheduler` | `AlarmManager.setExactAndAllowWhileIdle` when permitted; falls back to `setAndAllowWhileIdle` |
| `ReminderBootReceiver` | Reschedules all active reminders on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` |
| Exact-alarm banner | Shown on Reminders screen when `canScheduleExactAlarms()` is false (Android 12+) |

### Upcoming tab query

`RemindersUiState.filteredReminders` for **Upcoming**:

- `status == ACTIVE`
- `triggerAt >= now`
- Sorted ascending by `triggerAt`

Overdue and Completed tabs use the complementary filters documented in [DOCUFIND_REMINDER_ENGINE.md](./DOCUFIND_REMINDER_ENGINE.md).

See also [DOCUFIND_DATE_PICKER_STANDARD.md](./DOCUFIND_DATE_PICKER_STANDARD.md) and [DOCUFIND_NOTIFICATION_SYSTEM.md](./DOCUFIND_NOTIFICATION_SYSTEM.md).

---

## 3. OCR architecture (ML Kit)

| Topic | Detail |
|-------|--------|
| Engine | `MlKitOcrEngine` implements `OcrEngine`; DI via `OcrModule` / `TextRecognizerProvider` |
| Supported inputs | JPEG, PNG, PDF (PDF pages rasterized via `PdfOcrHelper`) |
| Limits | Same 10 MB vault file cap; unsupported MIME rejected by `OcrInputValidator`; images downsampled (max dimension 2048 px) |
| UX | Add Document → OCR runs with progress UI → `OcrReviewSheet` → user accepts or dismisses |
| Accepted text storage | `OcrAcceptedTextStorage` merges into **notes** (not tags) because `SearchIndexPolicy.allowsOcrTextInSearchIndex()` is `false` |
| Temp files | `OcrTempStore` under `cache/ocr/`; wiped per-run and on vault lock via `DocuFindApplication` |
| Search | OCR text is **never** indexed — policy enforced in `SearchIndexPolicy` |

Package map: `com.docufind.app.ocr.*`

---

## 4. Secure PDF export

| Step | Implementation |
|------|----------------|
| Auth | `AuthPurpose.DOCUMENT_EXPORT` — always re-auth (see §7) |
| Build | `WatermarkedPdfBuilder` (PdfBox) — logo watermark, header/footer, metadata sections |
| Filter | `PdfExportContentFilter` excludes PASSWORD fields and netbanking passwords |
| Encrypt | `EncryptedPdfWriter` — PdfBox `StandardProtectionPolicy`, 128-bit key, print allowed, extract/modify denied |
| Password rules | `PdfExportPasswordValidator` — required, minimum **8** characters, confirm must match |
| Cleanup | Plain PDF and temp decrypt files securely deleted; export cache wiped on vault lock |

Package map: `com.docufind.app.export.pdf.*`

---

## 5. QR scanner (CameraX + ML Kit)

| Topic | Detail |
|-------|--------|
| Camera | CameraX `Preview` + `ImageAnalysis`; ML Kit `BarcodeScanning` |
| Gallery fallback | `GetContent("image/*")` + ML Kit on selected image |
| Parsing | `QrContentParser` — phone, email, WhatsApp, vCard contact, http(s) URL, plain text |
| Safe actions | Confirmation dialog before external intents; `SafeUrlValidator` allows only `http`/`https` for open-URL |
| Phone dial | `Intent.ACTION_DIAL` with `tel:` URI (does not auto-place call) |
| Other actions | Copy, email (`ACTION_SENDTO`), WhatsApp (`wa.me`), save contact (`ACTION_INSERT`), open safe URL |

Route: QR scanner from Add Document flow → `QrScannerScreen` / `QrScannerViewModel`.

---

## 6. Vault auth — DOCUMENT_EXPORT always re-auth

`AuthGate.authenticateForPurpose`:

- `AuthPurpose.VAULT` may skip re-auth when `VaultSessionManager` is already unlocked.
- **`AuthPurpose.DOCUMENT_EXPORT` never skips** — PIN or biometric required every export, even if vault is unlocked.

Covered by `AuthGateTest` and used from `PdfExportManager` / `SecureFileAccessManager`.

---

## 7. Banking and Property field registries

Source: `CategoryFieldRegistry` in `domain/model/CategoryFieldSpec.kt`.

| Category ID | Display | Registry |
|-------------|---------|----------|
| `finance` | Banking/Netbanking | `bankingFields` — account details, IFSC/MICR, nominee, encrypted account number, optional netbanking username/password/URL; discretion warning |
| `property` | Property | `propertyFields` — type, ownership, address, registration, values, tax/maintenance due dates, notes |

Both categories appear on Home Quick Access (`QuickAccessItem.BANKING`, `QuickAccessItem.PROPERTY`).

Property primary dates: **Purchase date** (issue) and **Agreement renewal date** (expiry); reminders enabled by default.

See [DOCUFIND_CATEGORY_FIELD_SPEC.md](./DOCUFIND_CATEGORY_FIELD_SPEC.md).

---

## 8. Database — no schema bump

Banking and Property metadata use the existing `VaultRecord.tags` / `categoryMetadataJson` pattern via `CategoryFieldRegistry`. **Room schema remains version 12** — no migration required for these fields.

See [DOCUFIND_DATABASE_SCHEMA.md](./DOCUFIND_DATABASE_SCHEMA.md).

---

## 9. New dependencies and APK size

| Dependency | Version | License | Purpose |
|------------|---------|---------|---------|
| CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) | 1.4.1 | Apache-2.0 | QR live camera |
| ML Kit Text Recognition | 16.0.1 | Google Play services terms | OCR |
| ML Kit Barcode Scanning | 17.3.0 | Google Play services terms | QR decode |
| PdfBox-Android (`com.tom-roush:pdfbox-android`) | 2.0.27.0 | Apache-2.0 | Watermarked export + PDF encryption |

**APK impact:** These libraries add native/model payloads (ML Kit) and PdfBox assets. Expect a measurable increase over the prior ~41 MB debug APK baseline; run `assembleDebug` and compare output size before Play handoff. ProGuard/minify is still disabled in release build type.

Manifest additions: `CAMERA`, `SCHEDULE_EXACT_ALARM` (with inexact fallback).

---

## 10. Tests added (packages)

New or expanded JVM unit tests in:

| Package | Test classes |
|---------|--------------|
| `com.docufind.app.ocr` | `MlKitOcrEngineTest`, `PdfOcrHelperTest`, `OcrInputValidatorTest`, `OcrResultMapperTest`, `SearchIndexPolicyTest` |
| `com.docufind.app.export.pdf` | `EncryptedPdfWriterTest`, `PdfExportContentFilterTest`, `PdfExportPasswordValidatorTest`, `PdfExportFilenameSanitizerTest` |
| `com.docufind.app.unit.scan` | `QrContentParserTest` |
| `com.docufind.app.unit.security` | `AuthGateTest` (DOCUMENT_EXPORT re-auth) |
| `com.docufind.app.ui.screens.add` | `OcrAcceptedTextStorageTest` |
| `com.docufind.app.ui.screens.reminders` | `RemindersUiStateTest` (Upcoming tab filtering) |
| `com.docufind.app.unit.domain` | `CategoryFieldRegistryTest` (banking + property fields) |
| `com.docufind.app.unit.reminders` | `ReminderRepositoryImplTest` |

See [DOCUFIND_TESTING_STRATEGY.md](./DOCUFIND_TESTING_STRATEGY.md) and [DOCUFIND_TEST_COVERAGE_REPORT.md](./DOCUFIND_TEST_COVERAGE_REPORT.md).

---

## 11. Build commands

From repo root (Windows):

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind

# Compile app + unit test sources
.\gradlew.bat compileDebugKotlin

# Unit tests only (fast gate)
.\gradlew.bat testDebugUnitTest
# alias task: fastValidation

# Lint
.\gradlew.bat lintDebug

# Debug APK
.\gradlew.bat assembleDebug

# Full phase gate (assemble + unit + lint)
.\gradlew.bat fullValidation
```

`connectedDebugAndroidTest` remains a separate device/emulator step (not part of `fullValidation`).

---

## 12. Known limitations remaining

See [DOCUFIND_KNOWN_LIMITATIONS.md](./DOCUFIND_KNOWN_LIMITATIONS.md) for the full list. Highlights after this pass:

- OCR accuracy varies by scan quality; multi-language/handwriting not guaranteed
- OCR text stored in notes is not searchable via vault search index
- Exact alarm delivery depends on OEM settings and Android 12+ alarm permission
- QR scanner requires camera permission; no continuous scan history stored
- Secure PDF export is password-protected but user must manage exported file security
- ML Kit / CameraX increase APK size; no on-device model pruning in this pass
- English UI only; phone-first layouts

---

## 13. Manual regression checklist

Perform on a physical device after `assembleDebug` install. Full table in [DOCUFIND_FINAL_QA.md](./DOCUFIND_FINAL_QA.md#production-readiness-pass---2026-07-20).

| Area | Check |
|------|-------|
| Launcher icon | D + keyhole visible on round and squircle launchers; not cropped to blue disc |
| Reminders | Upcoming / Overdue / Completed tabs; custom reminder date+time picker; exact-alarm banner when disabled |
| Time change | Toggle system time or timezone → reminders still fire after reschedule |
| OCR | JPEG/PNG/PDF attachment → run OCR → accept text → saved in notes → not findable via search |
| Vault lock | After lock, OCR temp cache cleared (re-run OCR requires fresh extract) |
| PDF export | Re-auth required even when vault unlocked; password ≥ 8 chars; exported PDF opens only with password; watermark visible |
| QR scan | Camera + gallery; phone shows Call with confirmation → dialer (ACTION_DIAL); unsafe URL blocked |
| Banking / Property | Quick Access tiles; full field forms; discretion warning on Banking; property purchase/renewal dates |
| Auth | Export/share from unlocked vault still prompts PIN/biometric for PDF export |

---

## Key file index

```
app/src/main/
├── res/drawable/ic_launcher_foreground.xml
├── res/drawable/ic_launcher_monochrome.xml
├── res/drawable/ic_docufind_logo_reference.xml
├── java/com/docufind/app/
│   ├── ocr/                    MlKitOcrEngine, OcrTempStore, OcrModule
│   ├── export/pdf/             PdfExportManager, EncryptedPdfWriter, WatermarkedPdfBuilder
│   ├── scan/                   QrContentParser, SafeUrlValidator
│   ├── reminder/               ReminderAlarmScheduler, ReminderBootReceiver
│   ├── security/auth/          AuthGate, AuthPurpose
│   ├── domain/model/           CategoryFieldSpec (banking/property registries)
│   └── ui/
│       ├── components/         ReminderDateTimeField
│       └── screens/scan/       QrScannerScreen
```
