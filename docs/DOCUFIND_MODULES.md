# DocuFind — Core Modules

Phase 6 delivers polished **list + detail** screens for ten document modules. All data is local in SQLCipher Room + encrypted vault files.

---

## Module registry

Defined in `domain/model/module/DocuFindModule.kt`:

| Module | ID | Sub-categories |
|--------|-----|----------------|
| Documents | `documents` | Personal, Official Documents, Agreements, Bills & Receipts, Others |
| ID Cards | `id_cards` | Aadhaar, PAN, Passport, Driving License, Voter ID, Ration Card, Other ID |
| Cards | `cards` | Credit Card, Debit Card, Health Card, Employee Card, Membership Card, Other Card |
| Medical Records | `medical` | Medical reports, Health summary, Allergies, Doctor notes, Insurance cards |
| Prescriptions | `prescriptions` | Current, Past |
| Vaccination | `vaccination` | Child, Adult, Travel, Pet |
| Education | `education` | Marksheet, Certificate, Degree, Transcript |
| Insurance | `insurance` | Health, Life, Vehicle, Property |
| Vehicle | `vehicle` | 2 Wheeler, 4 Wheeler, Other |
| Warranty | `warranty` | Electronics, Appliance, Vehicle, Other |
| Banking | `finance` | Bank Statement, Tax Return, Investment, Loan, Netbanking |
| Property | `property` | Deed, Rent Agreement, Tax Receipt, Maintenance |

Each module defines **field definitions** for detail display (stored in record tags as `meta:key=value`).

---

## Navigation

```
Home Quick Access tile → category/{moduleId} → ModuleListScreen
Home Recent item → record/{recordId} → ModuleDetailScreen
More tile → category/more → ModuleHubScreen (grid of all modules)
FAB (+) on list → Add tab (New Document)
```

Unsupported categories (`family`, `pets`) still use dedicated screens; `family` and `pets` are not module list routes but have full form flows.

Home Quick Access includes **Banking** (`finance`) and **Property** (`property`) tiles added in the 2026-07-20 pass.

---

## List screen (`ModuleListScreen`)

Per module:

- **Top app bar** with back + module title
- **Search bar** — filters by title and sub-category
- **Filter chips** — All + each sub-category
- **Record cards** — title, sub-category, date, file count
- **Empty state** — *No records yet. Tap + to add your first [module] record securely.*
- **FAB** — navigates to Add tab

---

## Detail screen (`ModuleDetailScreen`)

- Module title, sub-category, updated date
- **Metadata fields** from module definition (parsed from tags)
- Issue / expiry dates
- Notes and user tags
- **Attached files** with Preview, Share, Download (decrypt to cache via FileProvider)
- **Edit** — title and notes (AlertDialog)
- **Delete** — secure wipe via `VaultRecordSecureDelete`

---

## Data layer

| Component | Role |
|-----------|------|
| `VaultRecordDao.observeModuleListRows()` | List with file counts (SQL subquery) |
| `VaultRecordRepository.observeModuleRecords()` | List flow |
| `VaultRecordRepository.observeRecordDetail()` | Record + files combined flow |
| `VaultRecordRepository.updateRecord()` | Edit title/notes/dates |
| `VaultRecordRepository.deleteRecord()` | Secure delete |
| `VaultFileExporter` | Decrypt file for preview/share |

---

## Module-specific fields (detail)

Fields are defined per module in `DocuFindModule.fieldDefinitions` and encoded in tags when saving (future Add flow enhancement). Examples:

- **Prescriptions:** doctor name, prescription date, medicines, dosage, refill date, follow-up date
- **Vaccination:** vaccine name, person/pet, dose number, date given, next due, clinic
- **Insurance:** policy name/number, provider, dates, premium, nominee
- **Vehicle:** type, number, insurance/PUC expiry, service record
- **Warranty:** product, brand, purchase/warranty dates

---

## UI components

| File | Purpose |
|------|---------|
| `ModuleRecordCard.kt` | List item card |
| `ModuleComponents.kt` | Filter chips, empty state |
| `ModuleHubScreen.kt` | All-modules grid |
| `ModuleListScreen.kt` / `ModuleListViewModel.kt` | List |
| `ModuleDetailScreen.kt` / `ModuleDetailViewModel.kt` | Detail |

---

## Strings

`module_search_hint`, `delete_record_*`, `edit_record_title`, `attached_files`, `preview`, `share`, `download`, `all_modules`

---

## Future work

- Pre-fill Add screen with module + sub-category from list FAB
- Full metadata edit forms per module
- PDF/image thumbnail in list cards
- Link family/pet modules to FamilyMember and Pet entities
