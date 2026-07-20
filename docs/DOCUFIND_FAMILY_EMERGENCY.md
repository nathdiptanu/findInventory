# DocuFind — Family & Emergency Contact Forms

Polished full-screen forms for managing family profiles and emergency contacts.

---

## Add Family Member

**Route:** `FamilyListScreen` / `FamilyDetailScreen` → FAB or Edit → `FamilyFormDialog` (full-screen scaffold)

| Field | Required | Control |
|-------|----------|---------|
| Profile photo | No | Circular avatar, **Add Photo** label, bottom sheet |
| Name | Yes | Text field (Words capitalization) |
| Relation | Yes | Bottom sheet with icons |
| Date of birth | No | Material 3 `DatePickerDialog` |
| Blood group | Yes (default Unknown) | Filter chips |
| Phone | No | Phone keyboard + validation |
| Email | No | Email keyboard + validation |
| Notes | No | Multi-line, sentence capitalization |

### Profile photo

- Tappable white card with 120 dp circular avatar and **Add Photo** / **Change Photo** label
- Tap opens bottom sheet:
  - **Take Photo** — requests `CAMERA` only when needed; shows friendly message if denied (no crash)
  - **Choose from Gallery** — Photo Picker (no storage permission on modern Android)
  - **Remove Photo** — shown when a photo exists
- When editing, existing encrypted avatar is decrypted to cache for preview
- Photos are encrypted via `SecureAttachmentStorage` on save

### Relation options (with icons)

Self, Spouse, Son, Daughter, Father, Mother, Brother, Sister, Grandfather, Grandmother, Guardian, Other

---

## Add Emergency Contact

**Route:** `EmergencyListScreen` → FAB → `EmergencyFormDialog` (full-screen scaffold)

| Field | Required | Control |
|-------|----------|---------|
| Name | Yes | Text field |
| Relation | Yes | Same icon bottom sheet (emergency styling on Guardian) |
| Phone number | Yes | Validated 7–15 digits |
| Alternate phone | No | Same phone validation when filled |
| Linked family member | No | Bottom sheet from saved family members |
| Notes | No | Multi-line |
| Primary contact | No | Switch with helper description |

Only one contact can be primary; saving a new primary clears others (`EmergencyContactRepository`).

Emergency relation sheet uses context-specific icons (e.g. Phone for Spouse, LocalHospital for Guardian).

---

## UI layout

- **Background:** `DocuFindBlueSurface` (light blue)
- **Sections:** White `DocuFindCard` blocks with 16 dp padding
- **Save:** Fixed bottom bar above navigation/gesture inset (`navigationBarsPadding`)
- **Back:** Top app bar arrow dismisses without saving

---

## Validation

| Rule | Message |
|------|---------|
| Name empty | Please enter a name. |
| Phone empty (emergency) | Please enter a phone number. |
| Phone invalid | Enter a valid phone number (7–15 digits). |
| Email invalid | Enter a valid email address. |

Validation runs in the form before calling the ViewModel; ViewModel retains a secondary guard.

---

## Database (v7)

| Table | Column |
|-------|--------|
| `emergency_contacts` | `linkedFamilyMemberId` (nullable FK-style string) |

Development uses `fallbackToDestructiveMigration()`.

---

## Shared components

| Component | Path |
|-----------|------|
| Form scaffold | `ui/components/form/DocuFindFormScaffold.kt` |
| Relation picker | `ui/components/form/DocuFindRelationPicker.kt` |
| Blood group chips | `ui/components/form/DocuFindBloodGroupSelector.kt` |
| Profile photo | `ui/components/form/DocuFindProfilePhotoPicker.kt` |
| Date field | `ui/components/DocuFindDateField.kt` |
| Validation | `ui/util/FormValidation.kt` |

---

## Manual QA checklist

1. Add family member with photo (camera + gallery), relation, DOB, blood group → Save
2. Reopen detail — fields persisted
3. Edit → Remove photo → Save
4. Cancel date picker — no crash, date unchanged
5. Add emergency contact with required phone, optional alt phone, linked member, primary toggle
6. Invalid phone shows inline error; Save blocked
7. Save button clears nav bar on gesture and 3-button navigation
