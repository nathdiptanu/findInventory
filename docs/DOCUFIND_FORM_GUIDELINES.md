# DocuFind — Form Guidelines

Standards for data-entry screens across Family, Emergency, Pets, and Documents modules.

---

## Layout

1. **Full-screen scaffold** — Use `DocuFindFormScaffold` instead of `AlertDialog` for multi-field forms.
2. **Background** — `DocuFindBlueSurface`; content in white rounded cards (`DocuFindFormSection`).
3. **Spacing** — 16 dp horizontal page padding; 16 dp between fields inside a card; 16 dp between cards.
4. **Save action** — Single primary button in a fixed bottom bar with `imePadding()` + `navigationBarsPadding()` so it stays above the keyboard and system nav.
5. **Scroll** — Form body scrolls; top bar and save bar remain fixed. Do **not** apply `imePadding()` to the scroll body (causes white gap with `adjustNothing`).

---

## Keyboard & insets

| Rule | Implementation |
|------|----------------|
| Window mode | `android:windowSoftInputMode="adjustNothing"` on `MainActivity` |
| Save bar | `Modifier.imePadding().navigationBarsPadding()` on bottom bar only |
| Scroll body | `verticalScroll` without `imePadding` |
| No fixed heights | Avoid hard-coded form heights; let content scroll |

Reference: `DocuFindFormScaffold.kt`, `NewDocumentScreen.kt` (save in `bottomBar`).

Support forms (`ReportBugScreen`, `SendFeedbackScreen`) use the same scaffold pattern.

---

## Fields

| Type | Pattern |
|------|---------|
| Text (name) | `KeyboardCapitalization.Words`, single line |
| Notes | `KeyboardCapitalization.Sentences`, min 2 lines |
| Phone | `KeyboardType.Phone`, 7–15 digit validation |
| Email | `KeyboardType.Email`, optional RFC-style check |
| Date | `DocuFindDateField` — disabled field + full-size overlay tap; M3 `DatePickerDialog`; Cancel dismisses without change; optional Clear when `allowClear = true` |
| Enum / relation | Bottom sheet with icon + label rows in `LazyColumn`, leading icon on field, checkmark on selected |
| Small enum set (blood group) | `FilterChip` row |
| Boolean | `Switch` with label, not bare checkbox |
| Photo | Tappable card with 120 dp circular avatar, **Add Photo** / **Change Photo**, bottom sheet for source |

---

## Capitalization

- **Screen titles:** Title Case — e.g. "Add Family Member", "Add Emergency Contact"
- **Field labels:** Sentence case — e.g. "Date of birth", "Phone number"
- **User input:** Words for names; Sentences for notes; never force ALL CAPS

---

## Validation

- Validate on Save tap; show **inline** `supportingText` on the offending field
- Use string resources (`validation_*`) — no hard-coded English in ViewModels for field errors
- Do not crash on invalid input; block save and keep form state
- Optional fields: validate format only when non-empty

---

## Permissions

- Request **only when the user chooses an action** that needs it (e.g. camera on "Take Photo")
- If permission denied: close sheet, show inline message on family form — no crash, no retry loop
- Gallery: prefer `PickVisualMedia` over legacy storage permissions

---

## Photos

1. Show circular placeholder with camera icon
2. Label **Add Photo** + hint "Tap to take or choose a photo"
3. Bottom sheet: Take Photo / Choose from Gallery / Remove Photo (if set)
4. Preview selected URI with `BitmapFactory` before save
5. Persist via repository + encrypted storage; support explicit remove

---

## Pickers & cancel behavior

- **Date:** Cancel = dismiss dialog, keep previous value
- **Relation / family member:** Tap outside or swipe down = dismiss, keep selection
- **Photo sheet:** Denied camera = dismiss sheet silently

---

## Add Document form

- **Category picker:** `DocuFindCategoryPicker` bottom sheet with icons
- **Sub-category / choices:** `DocuFindOptionPicker` bottom sheet (not cramped exposed dropdown)
- **Category fields:** `CategoryFieldsForm` grouped card — dynamic per category (see `DOCUFIND_CATEGORY_FIELD_SPEC.md`)
- **Dates:** `DocuFindDateField` — Material 3 `DatePickerDialog`; Cancel dismisses without crash
- **Attachments:** Multiple files per record via `AttachmentListSection`; camera, gallery (multi), PDF; 10 MB max each
- **Sensitive fields:** Masked input; encrypted at rest; masked on detail until vault unlock
- **Save:** Primary button at bottom of scroll with `imePadding()` + `navigationBarsPadding()`

---

## Do not

- Stack more than ~8 fields in a single card without grouping
- Use cramped dropdown menus for long lists (prefer bottom sheet)
- Place Save inside scroll content (always bottom bar)
- Clear optional dates when user taps Cancel

---

## Reference implementations

- Family: `ui/screens/family/FamilyFormDialog.kt`
- Emergency: `ui/screens/emergency/EmergencyFormDialog.kt`
- Shared: `ui/components/form/*`
