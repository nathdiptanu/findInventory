# DocuFind — Date Picker Standard

All date fields in DocuFind use a shared component for consistent UX and timezone-safe behavior.

---

## Component

**File:** `ui/components/DocuFindDateField.kt`

```kotlin
DocuFindDateField(
    label = "Issue date",
    epochMillis = state.issueDate,
    onDateSelected = { viewModel.onIssueDateChange(it) },
    allowClear = true  // optional dates only
)
```

---

## Behavior

| Rule | Implementation |
|------|----------------|
| Material 3 DatePicker | `DatePickerDialog` + `DatePicker` |
| No manual typing | `readOnly` + `enabled = false` field; tap opens dialog |
| Cancel safe | Dismiss button closes dialog without calling `onDateSelected` |
| Clear optional dates | When `allowClear = true` and value set, Clear button in dialog |
| Display format | `d MMM yyyy` in device locale (e.g. `28 Jun 2026`) |
| Placeholder | "Tap to pick a date" |

---

## Click handling

The field uses `enabled = false` on `OutlinedTextField` with disabled colors matching enabled appearance, plus a full-width `clickable` modifier. This avoids focus/scroll conflicts inside `LazyColumn` and matches the fix applied to family/emergency forms.

---

## Timezone conversion

Material DatePicker returns UTC midnight millis. DocuFind converts:

- **To picker:** local calendar date → UTC millis (`toUtcPickerMillis`)
- **From picker:** UTC millis → local start-of-day epoch (`fromUtcPickerMillis`)

This prevents off-by-one day bugs across time zones.

---

## Where used

| Screen | Fields |
|--------|--------|
| New Document | Issue date, Expiry date |
| Family form | Date of birth |
| Pet forms | DOB, vaccination dates |
| Custom reminders | Due date + time via `ReminderDateTimeField` |
| Reminders default time | Material `TimePicker` dialog (`ReminderTimePickerDialog`) |

## ReminderDateTimeField (2026-07-20)

**File:** `ui/components/ReminderDateTimeField.kt`

Combines category date picking with reminder time selection:

```kotlin
ReminderDateTimeField(
    dateMillis = date,
    onDateSelected = onDateSelected,
    timeMinutes = timeMinutes,
    onTimeMinutesChange = onTimeMinutesChange,
    showPastWarning = true
)
```

| Behavior | Detail |
|----------|--------|
| Date | Delegates to `DocuFindDateField` |
| Time | Outlined button opens Material 3 `TimePicker` in `ReminderTimePickerDialog` |
| Preview | Shows formatted date+time via `ReminderDateTimeFormatter` |
| Past warning | Error text when combined trigger is not in the future |

Used by custom reminder create/edit dialogs.

---

## Validation — expiry vs issue

On New Document, if expiry date is before issue date:

1. **On pick:** confirmation dialog before applying expiry
2. **On save:** same dialog if both dates already set

User must confirm to proceed; Cancel keeps previous expiry.

---

## Do not

- Use editable text fields for dates by default
- Clear date on Cancel
- Pass raw `selectedDateMillis` without UTC/local conversion
- Nest date dialogs inside non-interactive overlays that block taps

---

## Testing

- Open picker → select date → field shows formatted date
- Open picker → Cancel → value unchanged
- Clear on optional field → value null
- Pick expiry before issue → confirm / cancel flows
- Works inside scrollable forms (`LazyColumn`, dialogs)
