# DocuFind — Release Checklist

Use this checklist before publishing to Google Play or distributing a debug/release APK.

**Last validated:** 2026-07-20 (production-readiness documentation pass)  
**Vault stability validation:** 2026-07-01 - `./gradlew.bat assembleDebug` PASS.

---

## Pre-build

- [x] `DOCUFIND_KNOWN_LIMITATIONS.md` reviewed and current
- [x] `DOCUFIND_RELEASE_NOTES.md` updated for this version
- [x] Privacy policy text matches in-app Privacy screen
- [x] No debug-only endpoints or test credentials in source
- [x] Version code / name set in `app/build.gradle.kts` (1 / 1.0.0)
- [ ] ProGuard/R8 rules verified if minify enabled (release only — currently disabled)

---

## Quality gates

- [ ] `.\gradlew.bat compileDebugKotlin` — compile app + test sources
- [ ] `.\gradlew.bat testDebugUnitTest` / `fastValidation` — unit tests
- [ ] `.\gradlew.bat lintDebug` — 0 errors
- [ ] `.\gradlew.bat assembleDebug` — debug APK
- [ ] `.\gradlew.bat fullValidation` — assemble + unit + lint (phase sign-off)
- [x] Manual smoke test documented in `DOCUFIND_FINAL_QA.md`
- [x] Lint errors resolved (Vault scaffold padding; prior notification/alarm fixes)
- [x] Logcat review — profile camera FATAL fixed; cold start clean on device
- [x] Explicit Room migrations v1→9 (no destructive fallback)
- [ ] Fresh install + upgrade from previous DB version on device (recommended before store)
- [ ] Backup create → restore → restart on real device
- [ ] Notification permission + reminder fire on Android 13+ device
- [ ] `gradle assembleRelease` — when preparing store build

---

## UI / UX (regression)

- [ ] Production-readiness manual checklist (`DOCUFIND_FINAL_QA.md` § 2026-07-20): OCR, PDF export, QR scan, Banking/Property, exact-alarm banner
- [ ] OCR accepted text not findable via vault search
- [ ] PDF export re-auth when vault already unlocked
- [ ] Launcher icon on round + squircle OEM launcher
- [x] Splash vault unlock animation
- [x] Onboarding + profile setup
- [x] Home: username, rotating tagline, accent first letter, no bottom white gap
- [x] How to Use expand/collapse
- [x] Forms: `DocuFindFormScaffold`, keyboard gap fix, date/relation pickers
- [x] Reminders: add/edit/delete/actioned, test notification
- [x] Category picker bottom sheet
- [x] FAB does not cover last list item
- [ ] Dark mode spot-check (light-first design)
- [ ] TalkBack pass before store release

---

## Security

- [x] PIN lock and biometric gate on sensitive actions
- [x] Screenshot protection on sensitive screens (`ForceSecureScreenEffect`)
- [x] `SecureLogger` — no PIN/document plaintext in logs
- [x] Encrypted database (SQLCipher) and file storage
- [x] Backup files encrypted; password not stored
- [x] Explicit migrations + pre-migration backup (see `DOCUFIND_DATA_SAFETY.md`)
- [x] Share/download user-initiated only

---

## Functional flows (device)

- [x] Fresh install → splash → onboarding → profile → home
- [x] Vault: PIN setup, biometric enable/skip, unlock
- [x] Vault opening animation after unlock: "Opening your secure vault..." and "Preparing your protected documents..."
- [x] Vault module states: loading, empty, error with retry, content
- [ ] Manual smoke: Documents, ID Cards, Cards, Medical, Prescriptions, Vaccination, Education, Insurance, Vehicle, Warranty, Pets, Family, Emergency, Others/More
- [x] Add document + multiple files + category fields + date picker
- [x] Family + emergency forms (camera photo FileProvider fixed)
- [x] Reminders: add, edit, delete, actioned, test notification
- [x] Search + recent items (unlock gate) + settings sub-screens
- [x] Help & Support email intent
- [x] App restart + session lock

---

## Debug APK delivery

- [x] Generate debug APK only after QA gates pass
- [x] **APK path:** `app/build/outputs/apk/debug/app-debug.apk` (~41 MB)

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradlew.bat fullValidation
# or individually: compileDebugKotlin, testDebugUnitTest, lintDebug, assembleDebug
```

Install on connected device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Store assets (when publishing)

- [ ] App icon and feature graphic
- [ ] Screenshots (phone + optional tablet)
- [ ] Short and full description
- [ ] Data safety form: no collection, local storage only
- [ ] Content rating questionnaire

---

## Post-release

- [ ] Monitor support email: ask.artha.support@gmail.com
- [ ] Tag git release matching version name
- [ ] Archive backup of signing key (never commit keystore)
