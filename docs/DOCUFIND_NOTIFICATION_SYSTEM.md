# DocuFind — Notification System

Local notifications for reminders use **AlarmManager** + **NotificationCompat** on the **DocuFind Reminders** channel.

---

## Channel

| Property | Value |
|----------|--------|
| ID | `docufind_reminders` |
| Name | DocuFind Reminders |
| Importance | HIGH |
| Sound | Bundled `res/raw/docufind_notify` tone with gentle vibration pattern |
| Small icon | `ic_stat_docufind` (monochrome DocuFind D + keyhole) |

---

## Permission (Android 13+)

1. `POST_NOTIFICATIONS` declared in manifest.
2. Requested from Reminders screen when user taps the banner or bell icon.
3. `NotificationPermissionTracker` refreshes UI after permission dialog result and on screen resume.
4. `canPostNotifications()` checks **both** runtime permission and `NotificationManager.areNotificationsEnabled()`.

---

## Notification content

- **Title:** reminder title (e.g. insurance renewal)
- **Body:** notes or default “Tap to open in DocuFind”
- **Tap:** opens linked record after vault unlock via `ReminderPendingNavigation`

### Actions

| Action | Behavior |
|--------|----------|
| Mark actioned | Completes entire linked event group; cancels future offsets |
| Snooze | Reschedules this reminder +1 day |
| View record | Same as notification tap |

Handled by `ReminderActionReceiver` (broadcast).

---

## Test notification

Reminders screen → **Test notification** button:

- If permission denied → snackbar message
- If allowed → snackbar “in 5 seconds”, then sample notification on same channel with app icon

---

## Scheduling flow

```
AlarmManager → ReminderAlarmReceiver → ReminderNotificationHelper.showReminderNotification()
                                      → ReminderEngine.onReminderFired()
```

On app start and after boot, time set, or timezone change: `ReminderEngine.rescheduleAllActive()` re-registers future alarms.

`ReminderBootReceiver` handles:

- `android.intent.action.BOOT_COMPLETED`
- `android.intent.action.TIME_SET`
- `android.intent.action.TIMEZONE_CHANGED`

### Exact alarms (Android 12+)

- Manifest declares `SCHEDULE_EXACT_ALARM`.
- `ReminderAlarmScheduler.canScheduleExactAlarms()` drives the Reminders screen banner when exact scheduling is unavailable.
- Scheduler uses `setExactAndAllowWhileIdle` when permitted; falls back to `setAndAllowWhileIdle` on denial or `SecurityException`.

---

## Key files

```
reminder/ReminderNotificationHelper.kt
reminder/ReminderActionReceiver.kt
reminder/NotificationPermissionTracker.kt
reminder/ReminderAlarmReceiver.kt
res/drawable/ic_stat_docufind.xml
ui/screens/reminders/RemindersScreen.kt
MainActivity.kt (permission launcher)
DocuFindApplication.kt (channel + reschedule on start)
```

See also [DOCUFIND_REMINDER_ENGINE.md](./DOCUFIND_REMINDER_ENGINE.md).

## QA update - 2026-07-02

- Current channel ID is `docufind_reminders_v3`, named **DocuFind Reminders**.
- Channel sound uses the bundled `res/raw/docufind_notify` reminder tone.
- Notification actions are **Open**, **Mark Done**, and **Snooze**.
- Small icon uses the monochrome DocuFind notification drawable; large icon uses the branded DocuFind mark.
- Android 13+ posting is guarded by `POST_NOTIFICATIONS` and system notification enabled checks.
- Test notification fires after 5 seconds from the Reminders screen.
- One-time reminder rows are not auto-completed at fire time, so notification actions remain usable while no repeat alarm is scheduled.
- Linked expiry/renewal/refill schedules use the user-selected default notification time from the Reminders screen.

## Production readiness - 2026-07-20

- Exact-alarm availability banner on Reminders screen when `canScheduleExactAlarms()` is false.
- Reschedule receiver also listens for `TIME_SET` and `TIMEZONE_CHANGED` (in addition to boot).
