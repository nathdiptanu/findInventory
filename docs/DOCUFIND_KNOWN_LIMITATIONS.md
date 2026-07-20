# DocuFind — Known Limitations

This document lists intentional scope limits and known constraints as of the **2026-07-20 production-readiness pass**. Not every item is a bug; many reflect the local-first, privacy-focused product design.

## Platform & storage

| Limitation | Detail |
|------------|--------|
| Local only | Data stays on device unless user exports, shares, or saves a backup file |
| No cloud sync | No account system or multi-device sync |
| Single device restore | Backup restore replaces local vault; merge is not supported |
| File size | Attachments over 10 MB are rejected (add flow and bug-report screenshot) |
| Storage quota | Limited by device free space; no cloud offload |

## Security

| Limitation | Detail |
|------------|--------|
| Backup password | Not recoverable if forgotten; encrypted backup cannot be decrypted without it |
| Biometrics | Depends on device hardware and OS enrollment |
| Root / compromised devices | Standard Android app; cannot fully protect against rooted device extraction |
| Screenshot / recents | Sensitive screens use FLAG_SECURE; OS recents may still show non-sensitive UI when unlocked |

## Features

| Limitation | Detail |
|------------|--------|
| OCR | ML Kit OCR on Add Document (JPEG/PNG/PDF) is opt-in per attachment; accuracy varies; handwriting and non-Latin scripts not guaranteed; accepted OCR text goes to **notes only** and is **not** in the search index |
| OCR search | Vault search still uses title, category, tags, and metadata — not OCR body text |
| QR scanner | Requires camera permission; scans are not stored in the vault; only user-confirmed actions leave the app |
| PDF export | Password-protected export is user-managed after save; watermark does not prevent screenshot of opened PDF in external viewers |
| APK size | CameraX, ML Kit models, and PdfBox-Android increase download size vs pre–production-readiness builds (~41 MB debug baseline) |
| Collaboration | No shared family vault or multi-user access |
| Web / desktop | Android app only |
| Automatic backup | User must manually create and save backup files |
| Reminder delivery | Requires notification permission; exact alarm behavior varies by OEM and Android version |
| Email support | Requires installed email app; otherwise report text can be copied manually |

## UI / UX

| Limitation | Detail |
|------------|--------|
| Language | English UI strings only (no localization yet) |
| Tablet layout | Phone-first layouts; tablets use scaled phone UI |
| Dark mode | Follows system theme; some brand accents (e.g. pet FAB teal) stay fixed |
| PDF preview | Very large or corrupted PDFs may show preview unavailable |
| Filter chips | Horizontal scroll; long category names may truncate on very narrow screens |

## Database

| Limitation | Detail |
|------------|--------|
| Schema migrations | Explicit migrations v1→9; **no** destructive fallback. Pre-migration backup to `files/db_migration_backups/`. See [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md) |
| Upgrade testing | JVM migration tests + optional instrumented tests (`connectedDebugAndroidTest`); skipping many versions untested |
| Search index | Rebuilt on record changes; very large vaults may briefly lag on save |
| Migration failure | App shows safe error screen; user should not uninstall — backup preserved |

## Testing gaps

| Item | Notes |
|------|-------|
| OEM skins | Samsung, Xiaomi, etc. notification and battery optimizations not exhaustively tested |
| Full device regression | Automated gates pass; exhaustive tap-through on every OEM recommended before Play release |
| Low RAM devices | Not formally benchmarked below 2 GB RAM |
| Accessibility | Basic TalkBack pass recommended before store release; full WCAG audit not done |

## Reporting issues

Use **Settings → Report a Bug** or email **ask.artha.support@gmail.com**. Include steps to reproduce and Android version.
