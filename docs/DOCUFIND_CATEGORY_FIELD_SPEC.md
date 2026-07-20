# DocuFind — Category Field Specification

Category-specific metadata collected on the Add Document screen and stored in `VaultRecord.tags` as `meta:key=value` entries.

## Field types

| Type | UI | Storage |
|------|-----|---------|
| TEXT | Outlined text field | Plain in SQLCipher tags |
| DATE | `DocuFindDateField` (Material 3) | Epoch millis string in tags |
| CHOICE | `DocuFindOptionPicker` bottom sheet | Plain string |
| SENSITIVE | Masked text field | AES-GCM encrypted (`meta:key=enc:…`) |
| PASSWORD | Password field | AES-GCM encrypted |
| MULTILINE | Multi-line text | Plain in tags |

Sensitive values use `SensitiveMetadataCipher` (Keystore-wrapped metadata key). Detail screen shows `••••••` until vault is unlocked.

## Registry

Source of truth: `domain/model/CategoryFieldSpec.kt` → `CategoryFieldRegistry`

## Categories with custom fields

### Documents
Document type (choice), reference number, issue date, optional expiry date, notes, attachments.

### ID Cards
ID type (Aadhaar, PAN, Passport, Voter ID, Driving Licence, Other ID), name on ID, encrypted ID/reference number, issue date, expiry date, attachment.

### Cards
Card type (credit, debit, health, membership, employee, other), issuer, last 4 digits, encrypted card number optional, card expiry. Discretion warning shown.

### Medical
Record type (report, doctor note, allergy, diagnosis, health summary), patient, hospital/clinic, doctor, record date, allergies, diagnosis, health summary.

### Prescriptions
Doctor, patient, prescription date, medicine, dosage, frequency, refill/follow-up primary date, refill date, follow-up date.

### Vaccination
Vaccine, person/pet, date given, next due primary date, clinic, reminder note.

### Education
School/college, class/grade, academic year, record type (marksheet, certificate, activity record, admission, fee receipt, other), activity type.

### Banking/Netbanking (`finance`)

Account display name, bank name, account holder, account type (choice), account number (encrypted), IFSC/MICR, branch, nominee, customer ID (encrypted), netbanking password (encrypted optional), URL, contact details, notes. Discretion warning shown.

### Property (`property`)

Property title, type (Residential/Commercial/Land/etc.), ownership, owner(s), co-owner(s), address, city, state, postal code, registration number, purchase/estimated value, builder, seller, nominee notes, loan reference, insurance, tax due date, maintenance due date, notes. Primary dates: **Purchase date** and **Agreement renewal date**; reminders on by default.

### Insurance
Insurance type, provider, policy number (encrypted), start date, renewal/expiry date, premium, nominee.

### Vehicle
Vehicle type, number, RC details, insurance company, insurance/PUC/service dates.

### Pets
Pet type, name, breed, DOB/adoption, record type, vet contact, vaccination, medicine, insurance provider, next vaccination, medical notes.

### Warranty
Product, brand, purchase date, warranty expiry, invoice number.

### Family
Name, relation, DOB, blood group, phone, email, photo/document attachment note.

### Emergency
Name, relation, phone, alternate phone, notes.

### Others
Record type, reference number, important date, details.

## Date handling

- All primary category dates use `DocuFindDateField`; manual date typing is not exposed by default.
- Primary expiry/due labels are category-specific: card expiry, warranty expiry, renewal/expiry date, next due date, refill/follow-up date.
- Expiry-before-issue validation is enforced for top-level issue/expiry dates before save.
- Additional metadata dates also use `DocuFindDateField` through `CategoryFieldsForm`.

## Attachments

Each record supports **multiple** `VaultFile` entries (PDF/JPEG/PNG, max 10 MB each). Examples:
- Aadhaar front + back
- Policy + receipt + nominee form
- Marksheet + certificate

## Files

- `domain/model/CategoryFieldSpec.kt`
- `ui/components/form/CategoryFieldsForm.kt`
- `domain/model/module/RecordMetadata.kt`
- `security/metadata/SensitiveMetadataCipher.kt`
