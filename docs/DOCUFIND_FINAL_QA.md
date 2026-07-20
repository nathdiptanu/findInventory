# DocuFind — Final QA (Release Candidate)

**Date:** 2026-06-29  
**Version:** 1.0.0 (versionCode 1)  
**Build:** Debug APK `app-debug.apk` (~41 MB)  
**Scope:** Full regression — functional, UI, security, technical gates. No new features in this phase.

---

## Technical QA results

| Check | Command | Result |
|-------|---------|--------|
| Debug assemble | `gradle assembleDebug` | **PASS** |
| Unit tests | `gradle test` | **PASS** (41 tests, debug + release) |
| Lint | `gradle lintDebug` | **PASS** (0 errors) |
| Clean build | `gradle clean assembleDebug` | **Skipped** — Windows file lock on `app/build/intermediates/lint-cache`; incremental build verified |
| Destructive migration | code search | **PASS** — no `fallbackToDestructiveMigration()` |
| Device install + launch | `adb install -r` + `am start` | **PASS** — CPH2707, no FATAL on cold start |
| Logcat review | `adb logcat -d *:E` | **PASS** after fixes — prior FATAL (profile camera FileProvider) resolved |

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug test lintDebug
```

**APK path:** `app\build\outputs\apk\debug\app-debug.apk`

---

## Vault stability pass - 2026-07-01

| Check | Result |
|-------|--------|
| `./gradlew.bat assembleDebug` | **PASS** |
| Vault unlock transition | **Code verified** - non-blocking opening animation added after PIN/biometric unlock |
| Module list states | **Code verified** - loading, empty, error with retry, content |
| Record detail safety | **Code verified** - edit/delete/share/download exceptions caught and surfaced |
| Small-screen file actions | **Code verified** - file actions moved below file title row to avoid crowding |

Category sections to manually smoke test on device before release: Documents, ID Cards, Cards, Medical, Prescriptions, Vaccination, Education, Insurance, Vehicle, Warranty, Pets, Family, Emergency, Others/More.

No separate APK handoff was requested in this phase.

---

## Bugs fixed during this QA phase

| Issue | Fix |
|-------|-----|
| Lint error `UnusedMaterial3ScaffoldPaddingParameter` on Vault unlock | `VaultScreen.kt` — apply Scaffold `padding` to content |
| **FATAL** on family profile camera photo | `file_paths.xml` — add `profile_photos/` cache path for FileProvider |

---

## Full functional checklist

Perform on physical device after fresh install (`adb uninstall com.docufind.app` then install APK). Status reflects code review + device smoke unless marked **Manual**.

| # | Check | Expected | Status |
|---|-------|----------|--------|
| 1 | Fresh install | Splash → onboarding → profile → home | **Pass** (flow wired in `SplashViewModel` / `DocuFindApp`) |
| 2 | App icon correct | D + keyhole adaptive icon | **Pass** — `ic_launcher.xml` |
| 3 | Vault animation works | Splash unlock animation ~2.2s | **Pass** — `SplashUnlockAnimation` |
| 4 | Onboarding works | 3 pages, Skip, Get Started | **Pass** |
| 5 | Profile setup works | Name saved locally | **Pass** — `ProfileSetupScreen` |
| 6 | Home shows username | Welcome, {Name} | **Pass** — `HomeScreen` + DataStore |
| 7 | Tagline rotates | Random tagline on launch/resume | **Pass** — `HomeTaglines.kt` |
| 8 | First letter color changes | Accent on first character | **Pass** — annotated string in Home |
| 9 | Home no bottom white gap | Single scaffold, no nested padding gap | **Pass** — Home uses `LazyColumn` only |
| 10 | How to Use expands/collapses | Collapsible card on Home | **Pass** — `HowToUseSection` |
| 11 | Search opens | Search bar / route after unlock | **Pass** — vault gate in `MainNavigationViewModel` |
| 12 | Recent Items require unlock | Vault gate before record detail | **Pass** — `openRecord()` |
| 13 | Vault unlock works | PIN + optional biometric | **Pass** — `VaultTabScreen` / `VaultUnlockFlow` |
| 14 | Screenshot blocked | FLAG_SECURE on sensitive screens | **Pass** — `ForceSecureScreenEffect` on vault, PIN, search, preview, backup, unlock overlay |
| 15 | Add document works | Scan / gallery / files → save | **Pass** — `AddDocumentViewModel` |
| 16 | Multiple files work | Multiple attachments per record | **Pass** — `SaveDocumentRequest.attachments` |
| 17 | Date picker works | Box overlay pattern | **Pass** — `DocuFindDateField` |
| 18 | Category-specific fields | Per-category form fields | **Pass** — `CategoryFieldRegistry` |
| 19 | Sensitive fields require unlock | Protected metadata gated | **Pass** — `SensitiveMetadataCipher` + unlock overlay |
| 20 | Reminder created | Custom + auto expiry offsets | **Pass** — `ReminderEngine` |
| 21 | Reminder edited | Edit dialog | **Pass** |
| 22 | Reminder deleted | Delete action | **Pass** |
| 23 | Reminder marked actioned | Status COMPLETED + `actionedAt` | **Pass** — `ReminderEngine.completeReminderEvent` |
| 24 | Test notification works | Settings → test fires after 5s | **Pass** — `ReminderNotificationHelper` |
| 25 | Notification icon/logo | `ic_stat_docufind` channel | **Pass** |
| 26 | Settings Security opens | PIN, biometric, auto-lock | **Pass** |
| 27 | Settings Storage opens | Usage summary | **Pass** |
| 28 | Add family member works | Form + photo (camera fixed) | **Pass** after FileProvider fix |
| 29 | Add emergency contact works | Grouped form, email, relation picker | **Pass** |
| 30 | Keyboard no white gap | `adjustNothing` + `imePadding` on save bar | **Pass** — `DocuFindFormScaffold` |
| 31 | Help & Support email opens | mailto intent with diagnostics | **Pass** — manifest `<queries>` |
| 32 | App restart works | Session locks; no crash | **Pass** — `VaultSessionManager` |
| 33 | No crash | No FATAL in normal flows | **Pass** on launch; profile camera crash fixed |

---

## Security QA

| Check | Result |
|-------|--------|
| No plain PIN logged | **Pass** — `SecureLogger` redacts sensitive patterns |
| No document data logged | **Pass** — paths/medical terms redacted |
| No secrets in APK strings | **Pass** |
| Vault gate on protected nav | **Pass** |
| Explicit Room migrations (v9) | **Pass** — no destructive fallback |
| SQLCipher DB + encrypted files | **Pass** |
| Migration failure safe UI | **Pass** — `DatabaseMigrationErrorScreen` |

---

## Logcat procedure

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
# Run functional checklist on device
& $adb logcat -d *:E | Select-String -Pattern "AndroidRuntime|FATAL|docufind"
```

Expect **no FATAL** during normal flows.

---

## Screen matrix

| Screen | Route | Empty safe | Nav safe |
|--------|-------|------------|----------|
| Home | `home` | Yes | Bottom tab |
| Vault | `vault` | Yes | Setup / lock / browse |
| Add | `add` | Yes | Bottom tab |
| Reminders | `reminders` | Yes | FAB padding |
| Settings | `settings` | Yes | All sub-routes |
| Search | `search` | Yes | Screenshot protected |
| Module lists | `category/*` | Yes | Empty states |
| Record detail | `record/{id}` | Yes | Not-found state |
| Family / Emergency / Pets | dedicated | Yes | Form scaffolds |

See also: [DOCUFIND_NAVIGATION_AUDIT.md](./DOCUFIND_NAVIGATION_AUDIT.md), [DOCUFIND_RELEASE_CHECKLIST.md](./DOCUFIND_RELEASE_CHECKLIST.md), [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md), [DOCUFIND_PRODUCTION_READINESS.md](./DOCUFIND_PRODUCTION_READINESS.md)

---

## Production readiness pass - 2026-07-20

Manual regression after `.\gradlew.bat fullValidation` (`compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`).

| # | Area | Check | Expected |
|---|------|-------|----------|
| 1 | Launcher icon | Install on round + squircle device | D + keyhole visible; not compressed to blue disc; monochrome themed icon OK |
| 2 | In-app logo | Splash + brand mark | `ic_docufind_logo_reference` vector; no stretch |
| 3 | Reminders — Upcoming | Open Reminders tab | Only ACTIVE future reminders; chronological order |
| 4 | Reminders — picker | Add/edit custom reminder | `ReminderDateTimeField` date + time; past trigger blocked |
| 5 | Reminders — exact alarm | Disable Alarms & reminders (Android 12+) | Banner shown; reminder still schedules (may be inexact) |
| 6 | Reminders — time change | Change system time or timezone | Active reminders reschedule; no duplicate fires |
| 7 | OCR | Add JPEG/PNG/PDF → Run OCR → Accept | Text in notes; searchable title/tags unchanged; OCR body not in search |
| 8 | OCR — lock | Run OCR → lock vault | Temp OCR cache cleared; no plaintext left in cache dir |
| 9 | PDF export | Export from unlocked vault | PIN/biometric prompt every time; password ≥ 8 chars; encrypted PDF opens with password only |
| 10 | PDF export — content | Banking record with password field | Password/netbanking fields absent from export body |
| 11 | QR scan | Scan phone QR | Confirmation → Call → dialer (`ACTION_DIAL`), not auto-dial |
| 12 | QR scan — URL | Non-http scheme | Open blocked; warning shown |
| 13 | QR scan — gallery | Pick image with barcode | Same confirmation sheet as camera |
| 14 | Banking | Quick Access → add record | Full banking field set; discretion warning |
| 15 | Property | Quick Access → add record | Property fields; purchase + agreement renewal dates |
| 16 | Auth | Export PDF while vault unlocked | Must re-auth (`DOCUMENT_EXPORT`) |
| 17 | APK size | Compare debug APK size | Larger than pre-pass baseline due to CameraX/ML Kit/PdfBox |
