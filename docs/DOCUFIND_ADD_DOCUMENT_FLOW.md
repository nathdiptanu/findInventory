# DocuFind — Add Document Flow

The **New Document** screen is the primary entry for saving vault records from the bottom-nav **Add** tab.

---

## Entry point

```
Bottom nav Add → AddScreen → NewDocumentScreen
```

---

## Screen layout (top to bottom)

### 1. Source options

Three rounded cards in a row:

| Option | Icon | Action | Permission |
|--------|------|--------|------------|
| **Scan** | Camera | `TakePicture` + FileProvider | `CAMERA` requested only when Scan is tapped |
| **Gallery** | Image | `PickVisualMedia.ImageOnly` | None (system picker) |
| **File** | Document | `OpenDocument` (PDF, JPEG, PNG) | None (system picker) |

Component: `AddSourceOptionsRow.kt`

### 2. Helper card — Tag it two ways

- Title: *Tag it two ways*
- Text: *Choose a category and add details so you can find your document quickly later.*
- Mini-tiles: Category, Family link, Pet link

Component: `TagItTwoWaysSection.kt`

### 3. Form fields

| Field | Required | Control |
|-------|----------|---------|
| Document title | Yes | Text |
| Category | Yes | Bottom sheet picker with icons + search |
| Sub-category | When available | Exposed dropdown |
| Family member | No | Exposed dropdown |
| Pet | No | Exposed dropdown |
| Issue / start date | No | Material 3 DatePicker |
| Expiry / renewal date | No | Material 3 DatePicker |
| Notes | No | Multiline text |
| Tags | No | Comma-separated text |
| Reminder | No | Switch (creates reminder when expiry set) |

### 4. Attachment preview

- Thumbnail for images
- PDF icon for PDFs
- File name and formatted size
- Remove before save
- Max size **10 MB** — rejected with snackbar, no crash

### 5. Save securely

Primary button at bottom of form.

---

## Category picker

- **Component:** `DocuFindCategoryPicker.kt`
- Modal bottom sheet with icon per category
- Search field when list has 8+ categories
- See [DOCUFIND_CATEGORY_SYSTEM.md](./DOCUFIND_CATEGORY_SYSTEM.md)

---

## Date pickers

- **Component:** `DocuFindDateField.kt`
- Material 3 `DatePickerDialog`
- Read-only field; tap opens picker (no manual typing)
- Cancel dismisses without changing value
- Optional **Clear** for optional dates
- Expiry before issue date triggers confirmation dialog

See [DOCUFIND_DATE_PICKER_STANDARD.md](./DOCUFIND_DATE_PICKER_STANDARD.md).

---

## Save flow

```
User taps Save securely
  → Validate title + category
  → If expiry < issue: confirm dialog
  → If no PIN: SecuritySetupFlow overlay
  → If attachment present: confirm save dialog with preview
  → On confirm:
      1. Insert VaultRecord (SQLCipher Room)
      2. Encrypt + store file via VaultFileImporter
      3. Upsert search_index metadata
      4. Optional Reminder if toggle on + expiry date set
  → Snackbar: "Saved securely in DocuFind."
  → Navigate to record detail (via vault unlock gate)
  → Form resets
```

On import failure, the vault record is rolled back.

---

## File handling

| Source | Contract | MIME types |
|--------|----------|------------|
| Scan | `TakePicture` | JPEG |
| Gallery | `PickVisualMedia.ImageOnly` | JPEG, PNG |
| File | `OpenDocument` | PDF, JPEG, PNG |

- Max size: **10 MB** (`MAX_VAULT_FILE_BYTES`)
- Preview: sampled bitmap decode in `AttachmentHelper`
- Unsupported types: snackbar message, no crash

---

## Architecture

| Layer | Files |
|-------|-------|
| UI | `NewDocumentScreen.kt`, `AddDocumentViewModel.kt`, `AttachmentHelper.kt` |
| Components | `AddSourceOptionsRow`, `TagItTwoWaysSection`, `FilePreviewCard`, `DocuFindDateField`, `DocuFindCategoryPicker` |
| Domain | `VaultCategory.kt`, `SaveDocumentRequest`, `VaultRecordRepository` |
| Data | `VaultRecordRepositoryImpl` → DAOs + `VaultFileImporter` |

---

## Permissions & providers

- `CAMERA` — only when Scan is tapped
- `FileProvider` — `${applicationId}.fileprovider` for camera output URI
- Paths: `cache/camera/` (see `res/xml/file_paths.xml`)

---

## Validation checklist

- [ ] Add document without file
- [ ] Add image attachment
- [ ] Add PDF attachment
- [ ] Pick category (with search)
- [ ] Open issue and expiry date pickers
- [ ] Cancel date picker (value unchanged)
- [ ] Expiry before issue confirmation
- [ ] Save record → snackbar + detail screen
- [ ] Reopen saved record
- [ ] Reject file > 10 MB gracefully
- [ ] `gradle assembleDebug test`
