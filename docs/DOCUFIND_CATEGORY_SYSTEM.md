# DocuFind — Category System

Categories organize vault records and drive sub-category options, quick-access tiles, and search filters.

---

## Source of truth

`domain/model/VaultCategory.kt` — enum with `id`, `displayName`, and `subCategories`.

Icons: `ui/components/form/VaultCategoryIcons.kt` via `VaultCategory.icon()`.

Picker UI: `ui/components/form/DocuFindCategoryPicker.kt`.

---

## Categories and icons

| Category | ID | Icon |
|----------|-----|------|
| Documents | `documents` | Description / file |
| ID Cards | `id_cards` | Badge |
| Cards | `cards` | Credit card |
| Medical Records | `medical` | Medical services |
| Prescriptions | `prescriptions` | Pharmacy / pill |
| Vaccination | `vaccination` | Syringe |
| Education | `education` | Graduation cap |
| Insurance | `insurance` | Shield |
| Vehicle | `vehicle` | Car |
| Warranty | `warranty` | Verified / certificate |
| Pets | `pets` | Paw |
| Family | `family` | Groups |
| Emergency | `emergency` | Emergency |
| Property | `property` | Home |
| Travel | `travel` | Airplane |
| Finance/Banking | `finance` | Savings / bank |
| Others | `others` | More horiz |

**Total:** 17 categories

---

## Category picker UX

- Trigger: read-only `OutlinedTextField` showing selected name + leading icon
- Sheet: Material 3 `ModalBottomSheet` with rounded top corners
- Each row: icon + label + checkmark for selection
- Search: shown when category count ≥ 8; filters by display name (case-insensitive)
- Empty search: "No categories match your search."

Do **not** use plain text-only dropdowns for primary category selection.

---

## Sub-categories

When a category is selected, sub-category options come from `VaultCategory.subCategories`.

Rendered as Material 3 `ExposedDropdownMenuBox` on the New Document form.

Example — ID Cards: Aadhaar, PAN, Passport, Driving License, …

---

## Quick Access mapping

`QuickAccessItem` on the home dashboard maps to vault category IDs where applicable.

`FAMILY` quick-access tile links to the Family module list (people), not the `family` document category — both coexist:

- **Family quick access** → family members screen
- **Family category** → family-related documents (certificates, etc.)

---

## Module alignment

`DocuFindModule` covers core browse modules. Records saved with categories like Property, Travel, Finance, or Others use `VaultCategory` even when no dedicated module hub exists — they appear in Vault browse and search.

---

## Adding a category

1. Add enum entry in `VaultCategory.kt` with id, display name, sub-categories
2. Add icon mapping in `VaultCategoryIcons.kt`
3. Optionally add `QuickAccessItem` if shown on home grid
4. Update strings if needed
5. Document in this file and `DOCUFIND_ADD_DOCUMENT_FLOW.md`
