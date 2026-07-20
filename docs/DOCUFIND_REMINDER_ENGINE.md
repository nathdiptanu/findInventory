# DocuFind — Unified Reminder Engine

Phase 8 delivers a single **reminder engine** for medicine, renewals, vaccinations, and custom reminders — with deduplication, linked-record sync, and local notifications.

---

## Categories

| Category | Typical source |
|----------|----------------|
| Medicine | Medicine schedule (4 daily slots) |
| Prescription refill | Prescriptions module expiry |
| Prescription follow-up | Prescription tags / follow-up date |
| Vaccination | Vaccination module |
| Pet vaccination | Pet record (vaccination type) |
| Insurance renewal | Insurance documents |
| Vehicle insurance | Vehicle module |
| PUC renewal | Vehicle sub-category / tags |
| Warranty expiry | Warranty module |
| Passport / ID expiry | ID Cards module |
| Education | Education module |
| Custom | User-created via Reminders FAB |

---

## Reminder fields (Room v5)

| Field | Description |
|-------|-------------|
| `id` | Primary key |
| `title` | Display title |
| `linkedRecordId` | Vault record (nullable) |
| `linkedPetId` | Pet profile (nullable) |
| `linkedFamilyMemberId` | Family member (nullable) |
| `linkedPetRecordId` | Pet vaccination record (nullable) |
| `linkedMedicineId` | Medicine entity (nullable) |
| `category` | Reminder category enum name |
| `reminderDate` | Date at local midnight (epoch) |
| `reminderTimeMinutes` | Minutes from midnight |
| `triggerAt` | Full fire time (date + time) |
| `repeatType` | NO_REPEAT, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM |
| `importance` | NORMAL, IMPORTANT, URGENT |
| `status` | ACTIVE, COMPLETED, DISABLED |
| `sourceKey` | Unique dedupe key (unique index) |

---

## Default expiry schedule

For expiry/renewal linked records (insurance, vehicle, PUC, warranty, passport/ID, vaccination, pet vaccination, prescription refill), DocuFind creates **six** reminders per due date:

| Offset | Fire time |
|--------|-----------|
| 15 days before | 9:00 AM local |
| 7 days before | 9:00 AM local |
| 3 days before | 9:00 AM local |
| 2 days before | 9:00 AM local |
| 1 day before | 9:00 AM local |
| Due date | 9:00 AM local |

Source keys: `{prefix}:{offsetDays}` e.g. `record:{id}:expiry:15`, `petrec:{id}:7`.

Defaults live in `ReminderScheduleDefaults`. The notification time is user-changeable from the Reminders screen and is stored in local preferences.

---

## Rules

1. **No duplicates** — `sourceKey` unique index + `INSERT OR REPLACE` via `ReminderEngine.upsert()`
2. **Linked record date changes** — `syncVaultRecordExpiry()` / `syncPetVaccination()` rebuild offset set
3. **Linked record deleted** — status set to `DISABLED`, alarms cancelled
4. **Notifications** — `AlarmManager.setExactAndAllowWhileIdle()` at `triggerAt` when `canScheduleExactAlarms()`; otherwise `setAndAllowWhileIdle()`. Rescheduled on boot, `TIME_SET`, and `TIMEZONE_CHANGED` via `ReminderBootReceiver`, and on app start.
5. **Notification tap** — opens linked record after PIN unlock via `ReminderPendingNavigation`
6. **Mark actioned** — `completeReminderEvent()` completes all active reminders in the same event group (same linked record/pet record expiry series)
7. **Snooze** — reschedules single reminder +1 day
8. **Permission** — `POST_NOTIFICATIONS` + system notifications enabled; see [DOCUFIND_NOTIFICATION_SYSTEM.md](./DOCUFIND_NOTIFICATION_SYSTEM.md)

---

## Medicine defaults

| Slot | Time |
|------|------|
| Morning | 8:00 AM |
| Afternoon | 1:00 PM |
| Evening | 6:00 PM |
| Night | 9:00 PM |

Source keys: `medicine:{medicineId}:{SLOT}` with `DAILY` repeat.

---

## Reminders screen

Tabs: **Upcoming** · **Overdue** · **Completed**

- Upcoming: `ACTIVE` and `triggerAt >= now`
- Overdue: `ACTIVE` and `triggerAt < now`
- Completed: `status == COMPLETED`

FAB opens custom reminder dialog (category, date, time, repeat, importance).

Custom reminders and overdue reminders use **`ReminderDateTimeField`** (`DocuFindDateField` + Material 3 `TimePicker` dialog), not manual typing. Saves are blocked for blank titles and past trigger times. Editing an overdue reminder lets the user move it to a future date/time, then the existing alarm is cancelled and rescheduled with the new trigger.

When exact alarms are unavailable (Android 12+ without Alarms & reminders permission), a banner explains that reminders may arrive a few minutes late and links the user to system settings.

Permission banner only shown when notifications are actually disabled (permission + system setting).

---

## Architecture

```
ReminderEngine              — upsert, sync, multi-offset, completeReminderEvent, snooze
ReminderScheduleDefaults    — 15/7/3/2/1/0 day offsets using the user-selected default notification time
ReminderAlarmScheduler      — AlarmManager exact alarms + canScheduleExactAlarms()
ReminderAlarmReceiver       — show notification + advance repeat
ReminderActionReceiver      — mark actioned / snooze from notification
ReminderBootReceiver        — reschedule after BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED
ReminderNotificationHelper  — channel, icon, actions, test notification
NotificationPermissionTracker — refresh UI after permission changes
ReminderPendingNavigation   — deep link after unlock
ui/components/ReminderDateTimeField.kt — shared date + time picker for custom reminders
```

Integrated from:
- `VaultRecordRepositoryImpl` — document expiry reminders
- `PetRepositoryImpl` — pet vaccination reminders
- `VaultRecordSecureDelete` — disable on record delete

---

## Key files

```
domain/model/reminder/ReminderModels.kt
data/local/db/entity/Reminder.kt (v5)
reminder/ReminderEngine.kt
reminder/ReminderScheduleDefaults.kt
reminder/ReminderAlarmScheduler.kt
reminder/ReminderAlarmReceiver.kt
reminder/ReminderActionReceiver.kt
reminder/ReminderNotificationHelper.kt
ui/screens/reminders/RemindersScreen.kt
ui/screens/reminders/RemindersViewModel.kt
docs/DOCUFIND_REMINDER_ENGINE.md
docs/DOCUFIND_NOTIFICATION_SYSTEM.md
```

---

## QA update - 2026-07-02

Reminder sync now treats a Vault record as a source for multiple independent reminder families, each with the default 15/7/3/2/1/0 day schedule at the user-selected local notification time:

| Source | Source key prefix |
|--------|-------------------|
| Top-level expiry / due date | `record:{id}:expiry` |
| Vehicle insurance expiry | `record:{id}:vehicle_insurance` |
| Vehicle PUC expiry | `record:{id}:puc` |
| Warranty expiry metadata | `record:{id}:warranty` |
| Prescription refill | `record:{id}:refill` |
| Prescription follow-up | `record:{id}:followup` |
| Vaccination next due | `record:{id}:vaccination` |
| Pet next vaccination | `record:{id}:pet_vaccination` |

Notification actions are **Open**, **Mark Done**, and **Snooze**. One-time reminders remain active after firing so these actions can still work from the visible notification; they are not rescheduled unless their repeat type requires it.

## QA update - 2026-07-17

- Custom reminder edit/create now uses the same Material time picker pattern as the default reminder time setting.
- Repository validation rejects blank titles and past trigger times for custom reminder creates/updates.
- Updating a reminder cancels the previous alarm before scheduling the future trigger, preventing stale duplicate alarms.
- Changing the default notification time now reschedules active linked offset reminders to the selected local time instead of only affecting newly created records.
- Rapid duplicate Add Document saves are guarded at the ViewModel before repository writes.

## Production readiness - 2026-07-20

- **`ReminderDateTimeField`** replaces ad-hoc time rows for custom reminder create/edit (date field + Material `TimePicker` + formatted trigger preview + past-time warning).
- **Exact-alarm banner** on Reminders screen when `ReminderAlarmScheduler.canScheduleExactAlarms()` is false.
- **`ReminderBootReceiver`** manifest filters: `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` → `ReminderEngine.rescheduleAllActive()`.
- **Upcoming tab** filters `ACTIVE` reminders with `triggerAt >= now`, sorted ascending (`RemindersUiState.filteredReminders`).
