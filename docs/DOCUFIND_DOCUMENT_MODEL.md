# DocuFind — Document Model

## VaultRecord

One logical document entry in SQLCipher (`vault_records`).

| Field | Purpose |
|-------|---------|
| id | UUID primary key |
| title | User-facing name |
| category | Vault category id (e.g. `insurance`, `finance`) |
| subCategory | Optional sub-type |
| familyMemberId / petId | Optional links |
| issueDate / expiryDate | Standard dates |
| notes | Free text |
| tags | User tags + `meta:*` category fields |

## VaultFile (1:N)

Each record may have **multiple** encrypted files:

```
VaultRecord 1 ──< VaultFile N
```

| Field | Purpose |
|-------|---------|
| recordId | FK → vault_records |
| fileName | Original display name |
| mimeType | PDF, JPEG, PNG |
| fileSize | Bytes |
| encryptedPath | AES-GCM ciphertext on disk |
| thumbnailPath | Optional preview thumb |

Import: `VaultFileImporter.importFile()` per attachment on save.

## Metadata encoding

Category-specific fields → tags:

```
meta:student_name=Jane Doe
meta:policy_number=enc:<base64 ciphertext>
```

Parse/display: `RecordMetadata` + `CategoryFieldRegistry`.

## Save flow

`SaveDocumentRequest`:
- Core fields + `categoryFieldValues: Map<String, String>`
- `attachments: List<PendingAttachmentEntry>` (uri + pending metadata)

Repository inserts record, imports each file, builds search index, syncs reminders.

## Detail view

`ModuleRecordDetail.metadataFields` merges registry fields (with sensitive masking) and legacy `DocuFindModule` fields.

## Files

- `data/local/db/entity/VaultRecord.kt`, `VaultFile.kt`
- `domain/model/VaultCategory.kt` (`SaveDocumentRequest`)
- `data/repository/VaultRecordRepositoryImpl.kt`
