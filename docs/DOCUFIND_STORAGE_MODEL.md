# DocuFind — Storage Model

Local-only secure storage for vault files and attachments. **No server. No cloud. No AI.**

---

## Principles

1. **Metadata in Room (SQLCipher)** — titles, categories, dates, tags, file paths
2. **Bytes on disk (AES-256-GCM)** — PDF/JPG/PNG encrypted per file with Keystore-wrapped keys
3. **Off main thread** — all import, encrypt, decrypt, and delete operations use `Dispatchers.IO`
4. **Bounded memory** — chunked stream I/O; thumbnails decoded with `inSampleSize`, never full-resolution loads
5. **Fail safe** — corrupted or oversize files return errors; temp files wiped on failure; no crash

---

## Directory Layout

Under app `filesDir`:

```
vault/
├── {fileId}.enc          # AES-GCM ciphertext (+ IV prefix)
├── {fileId}.key          # Keystore-wrapped content encryption key
└── thumbnails/
    └── {fileId}.jpg      # Downscaled preview (images only)

import_temp/              # Staging during import (wiped after use)
```

Preview cache (wiped on lock): `cacheDir/secure_preview/`

---

## Supported File Types

| MIME | Extension |
|------|-----------|
| `application/pdf` | pdf |
| `image/jpeg` | jpg |
| `image/png` | png |

---

## File Size Limit

**Maximum: 10 MB** (`MAX_VAULT_FILE_BYTES = 10 × 1024 × 1024`)

If exceeded during import:

> File is larger than 10 MB. Please choose a smaller file.

Validation runs on declared size (when available) and while streaming bytes to staging.

---

## Import Pipeline

`VaultFileImporter.importFile()` on IO dispatcher:

```
InputStream
  → stageImport (stream to temp, size check, SHA-256)
  → generateThumbnail (images only, sampled decode)
  → encryptStagedFile (chunked AES-GCM → .enc + .key)
  → insert VaultFile row
  → wipe staging temp
```

On any failure:

- Delete partial `.enc` / `.key`
- Delete thumbnail if created
- Wipe staging temp
- Return `VaultImportResult.Error` (no throw to UI)

---

## Encryption

| Layer | Technology |
|-------|------------|
| Database | SQLCipher (passphrase from Android Keystore) |
| Files | AES-256-GCM per file, unique CEK wrapped by master key |
| Wire format | `[12-byte IV][ciphertext + auth tag]` |

Chunked APIs: `AesGcmCipher.encryptFile()` / `decryptFile()` — 8 KB buffer, no full-file RAM load.

Legacy byte-array encrypt/decrypt retained for small payloads (backup headers).

---

## Thumbnails

- Generated only for JPEG and PNG
- Max dimension: **256 px** via `BitmapFactory.Options.inSampleSize`
- Format: JPEG quality 80
- Failure is non-fatal (record still imports without thumbnail)

---

## Preview

`SecureFileCache.decryptForPreview()` decrypts to app cache using chunked `decryptToFile()`. Cache wiped on vault lock or next preview.

---

## Secure Delete

`VaultRecordSecureDelete.deleteRecord(recordId)`:

1. Wipe each `.enc`, `.key`, thumbnail
2. Wipe preview cache entries
3. Delete `vault_files` rows
4. Remove search index entry
5. Unlink reminders
6. Delete `vault_records` row

Uses secure overwrite (`SecureDelete.wipeFile`) before unlink.

---

## Key Classes

| Class | Package | Role |
|-------|---------|------|
| `VaultFileImporter` | `data.local.storage` | Import orchestration |
| `VaultFileValidator` | `data.local.storage` | MIME + size checks |
| `VaultStoragePaths` | `data.local.storage` | Directory paths |
| `VaultThumbnailGenerator` | `data.local.storage` | Sampled image thumbs |
| `EncryptedFileManager` | `security.file` | Stage, encrypt, decrypt, delete |
| `VaultRecordSecureDelete` | `security.file` | Record + file wipe |
| `SecureFileCache` | `security.file` | Preview temp files |

---

## Error Types

| Exception | When |
|-----------|------|
| `FileTooLargeException` | Size > 10 MB |
| `UnsupportedFileTypeException` | MIME not PDF/JPEG/PNG |
| `CorruptedFileException` | Read/decrypt failure |

All surfaced to UI as `VaultImportResult.Error` with user-safe messages.

---

## Strings

- `R.string.file_too_large` — size limit message
- `VaultFileImporter.FILE_TOO_LARGE_MESSAGE` — same text for programmatic use

---

## Future Phases

- Wire Add screen to `VaultFileImporter`
- Backup export/import using `backup_metadata` + `BackupEncryption`
- Storage usage screen from `VaultFileDao.observeTotalFileSize()`
