# DocuFind 1.0.3 — Release Notes

**Release name:** DocuFind 1.0.3 — UI polish, logo fix & Google Drive backup  
**versionName:** `1.0.3`  
**versionCode:** `4`  
**applicationId:** `com.nathdiptanu.docufind`  
**minSdk / targetSdk:** 26 / 35  
**Package / namespace:** `com.docufind.app`  
**Date:** 2026-07-21  

---

## Artifacts

| File | Type | Notes |
|------|------|--------|
| `releases/docufind-v1.0.3.aab` | Play App Bundle (signed) | Upload this to Google Play |
| `releases/docufind-v1.0.3-debug.apk` | Debug APK | Sideload / smoke test only |

### SHA-256

```
docufind-v1.0.3.aab
E03950969C20FA40963080492507DDB716651D2000C0A06C7E1CF7BE7D78AB8C

docufind-v1.0.3-debug.apk
224420308C4D2ADBEE9932C4A27ED74E3E2AC06468EBA4EAC742CA36C5179502
```

---

## Release notes (Play Store / user-facing)

**What’s new**

- Clearer launcher icon: DocuFind D + keyhole, properly centered on Android home screens  
- App logo shown in the top bar on main screens for consistent branding  
- Keyboard no longer covers action buttons when typing (better form UX)  
- Important flows (export password, save / duplicate confirm) open as full screens instead of small popups  
- Pull down on Home to refresh  
- Google account sign-in for backup, with option to switch or sign out  
- Upload an **encrypted** vault backup to Google Drive (locked on your device first)  
- Restore by opening your `.dfbackup` from Drive and entering your backup password  

**Security reminder**  
Documents stay encrypted on this device. Backups are encrypted before they leave the phone. DocuFind cannot recover a forgotten backup password.

---

## Release notes (internal / GitHub)

```
DocuFind 1.0.3 (4) — UI polish, logo fix & Google Drive backup

- Adaptive launcher icon from Play mark (transparent FG, white BG, safe-zone padding)
- In-app brand mark aligned with launcher (ic_docufind_mark_clean)
- DocuFindTopBar with logo on Settings / Backup / module list
- windowSoftInputMode=adjustResize + imePadding on form screens
- Full-screen export password / confirm + save/duplicate confirms
- Home pull-to-refresh
- Google account picker (sign in / switch / sign out)
- Encrypted .dfbackup share to Google Drive; restore via SAF from Drive
- Soft trash, favourites, OCR field suggestions, category PDF, search sort (from Phase 2)
- Room schema v13 (deletedAt)

Artifacts:
- releases/docufind-v1.0.3.aab
- releases/docufind-v1.0.3-debug.apk

Known notes:
- Uninstall/reinstall (or clear launcher cache) to see the new icon
- minifyEnabled still false on release
- Device smoke recommended before production claim
```

---

## What was fixed in this train (Phase 1 → 1.0.3)

| Area | Status |
|------|--------|
| Favourites write path | Fixed |
| Soft trash / recycle bin | Fixed (schema v13) |
| OCR → field / date suggestions | Fixed |
| Duplicate save warning | Fixed |
| Category PDF export | Fixed |
| Search sort | Fixed |
| Home expiring-soon | Fixed |
| Family linked documents | Fixed |
| Security Centre encryption copy | Fixed |
| Launcher / in-app logo alignment | Fixed (1.0.3) |
| Full-screen forms vs popups | Fixed (1.0.3) |
| Keyboard covering buttons | Fixed (1.0.3) |
| Google login + Drive encrypted backup | Added (1.0.3) |

### Intentionally deferred

- System ringtone picker UI  
- Family-first onboarding pages  
- R8 / minifyEnabled  
- Full Drive appDataFolder API (current flow: encrypted share + SAF restore)  
- Cloud sync of live vault (offline-first; only encrypted backup export)  

---

## Play Console upload checklist

1. Create a new release (Internal / Closed / Production).  
2. Upload **`releases/docufind-v1.0.3.aab`** only (versionCode **4**).  
3. Paste the **user-facing** release notes above.  
4. If you see a “devices no longer supported” warning, open the device list; ABIs are unchanged (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).  
5. After install: uninstall old build once so the launcher icon refreshes.  

---

## Prior versions (reference)

| Version | versionCode | Notes |
|---------|-------------|--------|
| 1.0.0 | 1 | Closed testing baseline |
| 1.0.1 | 2 | Phase 2 vault hardening (do not re-upload; superseded) |
| 1.0.2 | 3 | versionCode bump for Play (`docufind-phase2-v1.0.2.aab`) |
| **1.0.3** | **4** | **Current — use this AAB** |
