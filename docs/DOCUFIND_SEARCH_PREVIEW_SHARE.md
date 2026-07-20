# DocuFind Phase 9: Local Search, Preview & Sharing

Phase 9 adds vault-wide local search, in-app file preview, and user-controlled sharing/export. All operations stay on-device; nothing is uploaded automatically.

## Search

### Indexed fields

Each vault record is indexed in the `search_index` Room table via `SearchIndexBuilder`:

| Field | Source |
|-------|--------|
| Title | `VaultRecord.title` |
| Category | Module id + display title |
| Subcategory | `VaultRecord.subCategory` |
| Tags | `VaultRecord.tags` |
| Notes | `VaultRecord.notes` |
| Family member | Linked member name |
| Pet | Linked pet name |
| File names | Attached `VaultFile.fileName` values |
| Important dates | Issue / expiry dates (formatted for text match) |

Combined lowercase text is stored in `searchText` for fast `LIKE` queries.

**OCR text is never indexed.** `SearchIndexPolicy.allowsOcrTextInSearchIndex()` returns `false`; accepted OCR text is stored in record notes only (`OcrAcceptedTextStorage`).

### Search UI (`SearchScreen`)

- **Search bar** with IME search action; submits to recent searches
- **Recent searches** (optional, persisted in DataStore)
- **Filter chips**: Category, Family, Pet, Due soon, Expired, File type, Favorite
- **Results grouped by category** label
- **Empty states** for idle (hint) and no matches

### Filters (`SearchFilters`)

| Filter | Behavior |
|--------|----------|
| Category | Exact module id match |
| Family member | `familyMemberId` |
| Pet | `petId` |
| Due soon | Expiry within 30 days, not yet expired |
| Expired | `expiryDate` before now |
| File type | Primary attachment MIME (`application/pdf`, `image/jpeg`, `image/png`) |
| Favorite | `isFavorite = true` |

### Repository & indexing

- `SearchRepository` / `SearchRepositoryImpl` — query flow, recent searches, reindex
- Index updated on record save/update via `VaultRecordRepositoryImpl`
- Full reindex on app start (`DocuFindApplication`) after DB migration
- Index row deleted with record via `VaultRecordSecureDelete`

### Database

- Schema version **6** — expanded `SearchIndexEntity` columns

## Preview

### Route

`file_preview/{fileId}` → `FilePreviewScreen`

### Supported types

- **PDF** — rendered with Android `PdfRenderer` (page bitmaps in a scroll list)
- **Images** — `BitmapFactory` decode for `image/*` MIME types

### Fallback

If preview fails or type is unsupported:

> Preview unavailable. You can open or share this file.

Open, share, and download actions remain available when a decrypted temporary file can be prepared.

If the encrypted file row exists but the underlying encrypted file cannot be found or decrypted because the original legacy file is unavailable:

> File unavailable. The original file may have been moved or deleted.

### Security

- `SecureFileAccessManager.decryptFile()` requires vault unlock (`AuthPurpose.DOCUMENT_VIEW`)
- PIN unlock dialog shown when session is locked
- Screenshot protection enabled on preview screen

## Universal Preview Update - 2026-07-02

`file_preview/{fileId}` is now the universal attachment preview route for Vault records. It supports Documents, ID Cards, Prescriptions, Medical, Insurance, Vehicle, Warranty, Education, and any other Vault category with a `VaultFile` attachment.

Every preview surface now shows file metadata before the preview body:

- File name
- Size
- Type
- Created date
- Category

Actions available from the preview screen:

- Back
- Open in another installed app
- Share
- Download
- Delete attachment

Images render inline. PDFs render inline when the renderer succeeds; otherwise the screen shows the graceful preview-unavailable message while preserving Open/Share/Download. Missing encrypted files no longer crash the screen.

Family and Pet profile photos remain separate profile-photo flows rather than `VaultFile` records. Family photos continue to use the shared profile photo picker, and Pet photos now support preview, replace, explicit remove, and safe gallery-cancel handling.

## Share & export

- **Share** — Android share sheet (`Intent.ACTION_SEND`) via `FileProvider`; user picks destination app
- **Download** — decrypted copy written to public `Downloads/` folder
- **Secure PDF export** — password-protected watermarked PDF (see above); requires fresh vault auth every time
- **User control** — no background upload; sharing only when user taps Share
- Decrypted cache cleared when vault locks (`SecureFileCache`)

### Secure PDF export (2026-07-20)

Record export as an encrypted, watermarked PDF:

| Step | Detail |
|------|--------|
| Auth | `AuthPurpose.DOCUMENT_EXPORT` — always re-auth (PIN/biometric) |
| Password | Required, minimum 8 characters, confirm must match (`PdfExportPasswordValidator`) |
| Content | Metadata sections + attachment previews; PASSWORD and netbanking password fields excluded |
| Watermark | DocuFind logo + generation timestamp on each page (`WatermarkedPdfBuilder`) |
| Encryption | PdfBox `StandardProtectionPolicy`, 128-bit key; print allowed, extract/modify denied |
| Cleanup | Plain PDF and temp decrypt files securely deleted; export cache wiped on vault lock |

Available from record detail / preview export flows via `PdfExportManager`.

### Module detail integration

`ModuleDetailScreen` navigates to in-app preview. Share/download use the same unlock gate through `SecureFileAccessManager`.

## Key files

```
app/src/main/java/com/docufind/app/
├── data/local/db/entity/SearchIndexEntity.kt
├── data/local/db/dao/SearchIndexDao.kt
├── data/local/search/SearchIndexBuilder.kt
├── data/repository/SearchRepositoryImpl.kt
├── domain/model/search/SearchModels.kt
├── domain/repository/SearchRepository.kt
├── security/file/SecureFileAccessManager.kt
├── export/pdf/PdfExportManager.kt
├── ocr/MlKitOcrEngine.kt
├── ui/screens/search/SearchScreen.kt
├── ui/screens/search/SearchViewModel.kt
├── ui/screens/preview/FilePreviewScreen.kt
├── ui/screens/preview/FilePreviewViewModel.kt
└── ui/preview/PdfPreviewRenderer.kt
```

## Build

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug test
```

No APK/AAB packaging step required for phase verification.
