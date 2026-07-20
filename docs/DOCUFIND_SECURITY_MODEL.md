# DocuFind — Security Model

Production-grade security foundation for a local-only sensitive document vault.

## Principles

1. **Security-first** — No placeholder crypto; no debug-only bypasses in release.
2. **Defense in depth** — Keystore + SQLCipher + per-file encryption + session lock.
3. **Minimal exposure** — Decrypted data exists only in memory/cache for the minimum time required.
4. **Local-only** — No cloud, no telemetry, no automatic exfiltration.

---

## 1. Database Encryption (SQLCipher)

| Requirement | Implementation |
|-------------|----------------|
| AES-256 database | SQLCipher via `net.zetetic:sqlcipher-android` + Room `SupportFactory` |
| Key from Keystore | `DatabaseKeyManager` — random 256-bit key wrapped by Keystore master key |
| Never hardcoded | Key generated at first launch, stored wrapped in app-private file |
| Never logged | `SecureLogger` redacts sensitive patterns; key bytes never logged |
| Never exported | Wrapped key unusable without Keystore master key on device |
| Key rotation | `DatabaseKeyManager.rotateDatabaseKey()` re-wraps with current master key |

**Files:** `security/keystore/DatabaseKeyManager.kt`, `di/AppModule.kt`

---

## 2. Android Keystore

| Requirement | Implementation |
|-------------|----------------|
| Master key | AES-256-GCM in Android Keystore (`docufind_master_key_v1`) |
| Hardware-backed | Used when available; graceful software fallback |
| Key wrapping | `KeystoreManager.wrapKey()` / `unwrapKey()` for DB and file keys |
| No Base64 in code | Keys generated at runtime only |
| Rotation | `KeystoreManager.rotateWrappedKey()` for advanced rotation flows |

**Files:** `security/keystore/KeystoreManager.kt`

---

## 3. File Encryption

| Requirement | Implementation |
|-------------|----------------|
| AES-256-GCM | `AesGcmCipher` — random 12-byte IV prepended to ciphertext |
| Per-file key | Unique key per document, wrapped by Keystore |
| Auth tag | GCM 128-bit tag included in ciphertext |
| Metadata | `EncryptedFileMetadata` — id, mime, checksum, path (no plaintext content) |
| Supported types | JPG, PNG, PDF via `SupportedMimeType` |
| Temp deletion | `SecureDelete.wipeFile()` overwrites before delete on import temps |

**Files:** `security/file/EncryptedFileManager.kt`, `security/crypto/AesGcmCipher.kt`

---

## 4. Secure File Cache

| Requirement | Implementation |
|-------------|----------------|
| Preview-only decrypt | `SecureFileCache.decryptForPreview()` |
| Auto wipe | Cache wiped on vault lock (`DocuFindApplication` observer) |
| OCR temp wipe | `OcrTempStore.wipeAll()` on vault lock (same observer) |
| Post-preview | `SecureFileCache.wipeAll()` / `wipeFile()` |
| No persistent plaintext | Cache under `cache/secure_preview/` |

**Files:** `security/file/SecureFileCache.kt`

---

## 5. Biometric Security

| Requirement | Implementation |
|-------------|----------------|
| BiometricPrompt | `BiometricAuthManager` via AndroidX |
| Fingerprint / Face | `BIOMETRIC_STRONG` authenticator |
| Activity host | `MainActivity` extends `AppCompatActivity` (required for `BiometricPrompt`) |
| Enrollment | User-initiated only — setup screen or Settings toggle |
| Unavailable device | Enable hidden; PIN-only fallback |
| Gate before vault | `AuthGate` + `AuthPurpose.VAULT` — may skip when session already unlocked |
| Gate before document view | `AuthPurpose.DOCUMENT_VIEW` |
| Gate before export | `AuthPurpose.DOCUMENT_EXPORT` — **always** requires fresh PIN/biometric; never skipped when vault is unlocked |
| Gate before restore | `AuthPurpose.BACKUP_RESTORE` |
| Session unlock | Biometric success → `VaultSessionManager.unlock()` (no PIN re-hash) |

**Files:** `security/auth/BiometricAuthManager.kt`, `security/auth/AuthGate.kt`

See [DOCUFIND_APP_LOCK.md](./DOCUFIND_APP_LOCK.md) for deferred PIN setup and unlock UX.

---

## 6. PIN Protection

| Requirement | Implementation |
|-------------|----------------|
| 6-digit PIN | `PinManager.PIN_LENGTH = 6` |
| Never plaintext | PBKDF2-HMAC-SHA256 verifier (210k iterations) + salt |
| Secure storage | `EncryptedSharedPreferences` via `PinStorage` |
| Rate limiting | `LockoutManager` progressive tiers |

**Lockout tiers:**

| Failures | Action |
|----------|--------|
| 5 | 30 second lock |
| 10 | 5 minute lock |
| 15 | Require re-authentication |

**Files:** `security/pin/PinManager.kt`, `security/pin/LockoutManager.kt`, `security/crypto/Pbkdf2KeyDeriver.kt`

---

## 7. Screenshot Protection

Sensitive screens always block screenshots and recents preview via `FLAG_SECURE`.

| Screen | Implementation |
|--------|----------------|
| Unlock overlay / Vault | `ForceSecureScreenEffect()` |
| Document detail | `ModuleDetailScreen` |
| Document preview | `FilePreviewScreen` |
| Vault browse / Search | `SearchScreen` |
| PIN setup | `PinSetupScreen` |
| Backup / restore | `BackupScreen` |

`ForceSecureScreenEffect` wraps `ScreenshotProtection.enable()` for the activity window. The flag is managed by `SecureScreenController`, a per-activity reference-counted controller, so one protected composable disposing during navigation cannot clear `FLAG_SECURE` while another protected screen is still visible. Default is **blocked**; no user override on sensitive flows yet.

**Files:** `security/protection/SecureScreenController.kt`, `security/protection/ForceSecureScreenEffect.kt`, `security/protection/ScreenshotProtection.kt`

See [DOCUFIND_PROTECTED_ACCESS_RULES.md](./DOCUFIND_PROTECTED_ACCESS_RULES.md) for the full screen list.

## 7a. Home Preview Minimization

Home Recent Items no longer render sensitive record titles or updated timestamps before vault authentication. The row shows a generic protected label, category context, and lock affordance, then routes through the existing vault gate on tap.

**Files:** `ui/components/RecentItemsSection.kt`, `ui/navigation/MainNavigationViewModel.kt`

---

## 7b. Protected Access & PIN Reset

All sensitive navigation goes through `MainNavigationViewModel.openRecord()` / `openPet()` / `openSearch()`. If the vault session is locked, `UnlockOverlay` shows the shared `VaultUnlockFlow` (navy gradient, shield badge, PIN keypad, optional biometric).

**Forgot PIN:**

| Biometric enabled | Flow |
|-------------------|------|
| Yes | Verify with biometric → clear PIN verifier → `SecuritySetupFlow` for new PIN |
| No | Explicit warning dialog → user confirms → clear PIN → new PIN setup |

Data is never silently deleted. `PinManager.clearPinForReset()` clears only the PIN verifier and lockout state.

**Files:** `ui/components/VaultUnlockFlow.kt`, `ui/navigation/MainNavigationViewModel.kt`, `security/auth/FriendlyAuthMessages.kt`

See [DOCUFIND_PROTECTED_ACCESS_RULES.md](./DOCUFIND_PROTECTED_ACCESS_RULES.md) and [DOCUFIND_APP_LOCK.md](./DOCUFIND_APP_LOCK.md).

---

## 8. Clipboard Protection

- `ClipboardProtection.copyWithAutoClear()` — clears after 60 seconds
- Document files never copied to clipboard automatically

**Files:** `security/protection/ClipboardProtection.kt`

---

## 9. Secure Logging

- `SecureLogger` — no sensitive logs in release; debug logs sanitized
- Never log: PIN, passwords, paths, document numbers, medical data, biometrics
- No stack traces with file paths in release

**Files:** `security/logging/SecureLogger.kt`

---

## 10. Export Security

- `ExportConfirmationDialog` — explicit user confirmation
- Message: *"You are exporting a decrypted copy. Store it securely."*
- Requires `AuthPurpose.DOCUMENT_EXPORT` before export executes — **re-auth every time**, even if vault session is unlocked (`AuthGate`)
- **Secure PDF export** (`PdfExportManager`): watermarked PdfBox build → `StandardProtectionPolicy` encryption; password min 8 chars; PASSWORD/netbanking fields stripped from body; temp files wiped on failure and vault lock

**Files:** `security/export/ExportSecurity.kt`, `export/pdf/PdfExportManager.kt`, `export/pdf/EncryptedPdfWriter.kt`, `ui/components/ExportConfirmationDialog.kt`, `ui/components/ExportPasswordDialog.kt`

---

## 11. Backup Encryption

| Requirement | Implementation |
|-------------|----------------|
| AES-256-GCM | `BackupEncryption` |
| Backup password | User-provided; wiped from memory after use |
| PBKDF2 | 210k iterations, random 32-byte salt |
| Random IV | Per backup via `AesGcmCipher` |
| Never store password | Password never persisted |
| Checksum | CRC32 wrapper via `BackupValidator.wrapWithChecksum()` |

**Files:** `security/backup/BackupEncryption.kt`

---

## 12. Restore Validation

Before restore overwrites live data:

1. Checksum validation
2. Backup version check
3. Schema version check
4. Metadata sanity (file count bounds)
5. Abort on corruption — `RestoreManager.validateBeforeRestore()`

**Files:** `security/backup/BackupValidator.kt`, `security/backup/RestoreManager.kt`

---

## 13. Memory Safety

- `SecureMemory.wipe()` for byte/char arrays after crypto operations
- Password char arrays cleared in `Pbkdf2KeyDeriver` and `BackupEncryption`
- File keys wiped after encrypt/decrypt in `EncryptedFileManager`

**Files:** `security/crypto/SecureMemory.kt`

---

## 14. Secure Delete

`DocumentSecureDelete.deleteDocument()` removes:

- Database record (`DocumentDao.deleteById`)
- Encrypted file + key file + thumbnail
- Preview cache entry
- Search index entry
- Reminder links

Overwrites file bytes before deletion via `SecureDelete.wipeFile()`.

**Files:** `security/file/DocumentSecureDelete.kt`, `security/file/EncryptedFileManager.kt`

---

## 15. Root Detection

`RootDetector` checks: su paths, Magisk, Busybox, hooking frameworks (Xposed/Substrate).  
**Warn only — does not block.**

Optional developer mode warning.

**Files:** `security/detection/DeviceSecurityDetector.kt`

---

## 16. Emulator Detection

`EmulatorDetector` — warns when running on emulator. Does not block.

---

## 17. Screen Timeout

`AutoLockTimeout` options: 30s, 1m, 2m, 5m, Always locked.  
`ScreenTimeoutManager` locks vault after inactivity.

**Files:** `security/settings/SecuritySettings.kt`, `security/session/ScreenTimeoutManager.kt`

---

## 18. App Background Lock

`AppLifecycleObserver` — locks vault immediately on `onStop` (app backgrounded).  
Requires PIN/biometric on resume.

**Files:** `security/session/AppLifecycleObserver.kt`, `DocuFindApplication.kt`

---

## 19. Search Index

- `SearchIndexEntity` — title, category, tags only
- `SearchIndexPolicy` — never index decrypted document body, **OCR text**, or extracted document numbers
- Accepted OCR text is stored in record **notes** only (`OcrAcceptedTextStorage`); `allowsOcrTextInSearchIndex()` returns `false`

**Files:** `data/local/db/entity/SearchIndexEntity.kt`, `security/SearchIndexPolicy.kt`

---

## 20. Support Emails

`SupportDiagnostics` includes only:

- Android version
- Device model
- App version
- Crash summary (user-provided)

Never attaches logs, database, files, or encrypted data.

**Files:** `security/export/ExportSecurity.kt`

---

## 21. GDPR-Style Privacy

`PrivacyScreen` in Settings explains:

- Everything stays on device
- No cloud, analytics, ads, tracking, telemetry, or server storage

**Files:** `ui/screens/privacy/PrivacyScreen.kt`

---

## 22. Security Tests

| Test | File |
|------|------|
| AES-GCM round-trip | `AesGcmCipherTest` |
| Wrong key / missing IV | `AesGcmCipherTest` |
| PBKDF2 verify / wrong PIN | `Pbkdf2KeyDeriverTest` |
| Backup encrypt/decrypt | `BackupEncryptionTest` |
| Wrong backup password | `BackupEncryptionTest` |
| Corrupted checksum | `BackupEncryptionTest` |
| Memory wipe | `SecureMemoryTest` |
| Auth gate export re-auth | `AuthGateTest` |
| OCR search policy | `SearchIndexPolicyTest`, `OcrAcceptedTextStorageTest` |
| PDF export password/filter | `PdfExportPasswordValidatorTest`, `PdfExportContentFilterTest` |
| QR content parsing | `QrContentParserTest` |

Run: `./gradlew test`

---

## Future Security Features (Planned)

- **Panic Lock** — one-tap instant lock
- **Decoy Vault** — secondary PIN opens harmless vault
- **Break-in Alerts** — local failed-unlock log (date/time)
- **Document Integrity** — periodic checksum verification
- **Backup Health Check** — verify backup readability post-create
- **Security Dashboard** — score from PIN, biometric, backup, lock settings

---

## File Preview and Attachment Handling Update - 2026-07-02

New Vault uploads are staged from the Android picker/camera result and copied into app-private encrypted vault storage. Saved records do not rely on long-lived external content URIs for normal preview/share/delete behavior.

`SecureFileAccessManager` decrypts attachments only after the Vault access gate succeeds. Decrypted preview/open/share files are temporary cache artifacts and remain under app control until Android hands them to a user-selected target through `FileProvider`.

If an encrypted file is missing or cannot be recovered, the preview screen shows:

> File unavailable. The original file may have been moved or deleted.

Single-file deletion now uses secure delete for the encrypted payload, key sidecar, thumbnail, preview cache, and database row. Family and Pet profile photos use encrypted local profile-photo storage and support explicit replace/remove without exposing raw external URIs after save.

## Security Hardening Audit - 2026-07-03

Audit result:

- Database encryption: SQLCipher remains enabled through `SupportOpenHelperFactory` with a Keystore-wrapped database key.
- File encryption: Vault attachments and profile photos remain stored in app-private encrypted storage.
- PIN storage: no plaintext PIN storage; PIN verifier uses PBKDF2 salt/hash inside encrypted preferences.
- Biometric flow: biometric unlock gates the Vault session and does not expose biometric secrets to app storage.
- Screenshot blocking: protected screens use `FLAG_SECURE`; Security Settings was added to the protected set.
- Backup security: backups are password-derived AES-GCM payloads with checksum validation; restore validates schema before replacing local data.
- Sensitive fields: SENSITIVE/PASSWORD fields are encrypted into metadata tags and now render only through the unlock-aware `RecordMetadata` path.
- Logs: app code uses `SecureLogger`, which emits debug-only sanitized messages and strips throwable details.
- Migrations: database version 11 adds a non-destructive search-cache sanitation migration; no destructive migration fallback is configured.

Known residual risk: user-entered freeform titles, notes, and non-sensitive user tags can still contain sensitive information because the app cannot reliably classify arbitrary text without over-blocking normal use. The search cache no longer stores notes or file names, and sensitive category fields are encrypted.

---

## Package Map

```
com.docufind.app.security/
├── crypto/          AesGcmCipher, Pbkdf2KeyDeriver, SecureMemory
├── keystore/        KeystoreManager, DatabaseKeyManager
├── pin/             PinManager, PinStorage, LockoutManager
├── auth/            AuthGate, BiometricAuthManager, AuthPurpose
├── session/         VaultSessionManager, AppLifecycleObserver, ScreenTimeoutManager
├── file/            EncryptedFileManager, SecureFileCache, DocumentSecureDelete
├── backup/          BackupEncryption, BackupValidator, RestoreManager
├── protection/      ScreenshotProtection, ClipboardProtection
├── detection/       RootDetector, EmulatorDetector
├── export/          ExportSecurity, SupportDiagnostics
├── settings/        SecurityPreferences, AutoLockTimeout
└── logging/         SecureLogger
```
