# DocuFind — Known Bugs and Fixes

## Vault stability pass (2026-07-01)

| Issue | Fix |
|-------|-----|
| Vault could appear blank immediately after unlock | `VaultTabScreen` now shows a lightweight shield/document opening animation while unlocked content prepares. |
| Category list flow errors could crash the screen | `ModuleListViewModel` converts repository errors into UI state; `ModuleListScreen` shows Error with Retry. |
| Category list had no explicit loading state | Module lists now support Loading, Empty, Error with Retry, and Content states. |
| Record edit/delete/share/download exceptions could escape coroutine actions | `ModuleDetailViewModel` wraps these operations and surfaces user-facing errors. |
| File action row could crowd on small screens | File actions now sit below the file title row to reduce overlap risk. |

### Validation

- `./gradlew.bat assembleDebug` - PASS
- Manual device smoke still required for all Vault categories before production sign-off.

## Brand & first-run polish (2026-06-28)

| Area | Change |
|------|--------|
| App icon | Replaced with approved **D + keyhole** adaptive icon (white squircle bg) |
| Splash | Vault/key unlock animation ~2.2s (`SplashUnlockAnimation`) |
| Onboarding | Premium copy; safe-area fix — Get Started no longer overlaps nav bar |
| Profile | New **Let's Personalize DocuFind** screen after onboarding (local only) |
| Home | **Welcome, {Name}** + rotating tagline with accent first letter |
| Bottom nav | Reminders label single-line ellipsis (no wrap) |
| Portrait | `screenOrientation="portrait"` on MainActivity |

### Validation

- `gradle assembleDebug test` — PASS
- Fresh install: splash → onboarding → profile → home
- No APK generated in this phase (per request)

See [DOCUFIND_BRAND_GUIDELINES.md](./DOCUFIND_BRAND_GUIDELINES.md)

## Launch crash fix (2026-06-28)

| Issue | Fix |
|-------|-----|
| FATAL on cold start: `Theme.AppCompat theme required` | `themes.xml` — `Theme.DocuFind` parent changed from `android:Theme.Material` to `Theme.AppCompat.Light.NoActionBar` |

## Navigation audit (2026-06-28)

Full route audit documented in [DOCUFIND_NAVIGATION_AUDIT.md](./DOCUFIND_NAVIGATION_AUDIT.md).

| Issue | Fix |
|-------|-----|
| Vault tab does not open | `VaultTabScreen`: no PIN → setup; locked → unlock; unlocked → browse |
| Recent Items crash | `openRecord()` vault gate; blank record ID ignored on Home |
| Search crash | Correct `DocuFindSearchBar` parameter order; empty DB safe |
| Search after unlock (blank query) | `pendingOpenSearch` flag in `MainNavigationViewModel` |
| Settings sub-screens crash | All routes registered; reliable `SettingsListItem` taps |
| How to Use crash | Dedicated `how_to_use` route; home uses horizontal `LazyRow` |
| Date picker crash | `DocuFindDateField` Box overlay (disabled field + tap layer) |
| Record detail cast crash | `rememberFragmentActivity()` instead of context cast |
| Missing / deleted record | `DocuFindAsyncContent` not-found state |
| Search back navigation | Optional back arrow on `search` route |

### Validation

- `gradle assembleDebug test`
- Manual tap-through checklist in [DOCUFIND_NAVIGATION_AUDIT.md](./DOCUFIND_NAVIGATION_AUDIT.md)

## Home dashboard crash fixes (2026-06-28)

| Issue | Fix |
|-------|-----|
| Search screen crash | `DocuFindSearchBar` parameters were swapped in `SearchScreen.kt` |
| How to Use crash | Replaced `LazyVerticalGrid` inside home scroll with horizontal `LazyRow` |
| Recent items crash | Navigate via `openRecord()` with vault gate; guard blank record IDs |
| Quick Access Reminders | Routes to Reminders tab, not invalid category |
| Home search empty query | Opens Search screen even with blank query (after unlock) |

## Home polish (2026-06-28)

| Issue | Fix |
|-------|-----|
| Large white gap above bottom nav | Removed nested `Scaffold` from `HomeScreen`; single `LazyColumn` in Main scaffold |
| “Reminders” label broken mid-word | `softWrap = false`, single line, ellipsis, smaller 11sp label |
| How to Use white patches / overlap | Replaced horizontal `LazyRow` with vertical step list + dividers |
| Missing DocuFind header | Added app name above Welcome greeting |
| Short tagline pool | Expanded to 100+ taglines in `HomeTaglines.kt`; random on launch/resume |
| Recent items minimal | Added category icon, category label, formatted date |
| Missing Banking / Property tiles | Added to `QuickAccessItem.homeGridOrder` |

See [DOCUFIND_HOME_SCREEN.md](./DOCUFIND_HOME_SCREEN.md) and [DOCUFIND_TAGLINE_SYSTEM.md](./DOCUFIND_TAGLINE_SYSTEM.md).

## Add Document flow (2026-06-28)

| Issue | Fix |
|-------|-----|
| Single file per record | Multiple attachments in `SaveDocumentRequest.attachments`; loop import in repository |
| Generic category fields | `CategoryFieldRegistry` + `CategoryFieldsForm` per category |
| Cramped dropdowns | `DocuFindOptionPicker` bottom sheet for sub-category and choice fields |
| Sensitive data in tags | `SensitiveMetadataCipher` + masked detail until unlock |
| Date picker cancel crash | `DocuFindDateField` dismiss-only on cancel |
| Keyboard overlap on Add | `imePadding()` on Add form scroll |

See [DOCUFIND_CATEGORY_FIELD_SPEC.md](./DOCUFIND_CATEGORY_FIELD_SPEC.md) and [DOCUFIND_DOCUMENT_MODEL.md](./DOCUFIND_DOCUMENT_MODEL.md).

## Security / vault unlock (2026-06-28)

### Issues found

| # | Issue |
|---|--------|
| 1 | PIN screen UX not aligned with Material 3 reference |
| 2 | Biometric Enable did nothing |
| 3 | No system biometric prompt (permission/enrollment) |
| 4–5 | Vault unlock broken; crashes after PIN/biometric |
| 6 | “Unlock required” title instead of **Unlock** |
| 7 | Vault tab did not open correctly |
| — | PIN forced on first launch (wrong flow) |
| — | `MainActivity` was `ComponentActivity` → `FragmentActivity` null → biometrics disabled/crash |

### Root cause

`BiometricPrompt` requires `FragmentActivity`. `MainActivity` extended `ComponentActivity`, so `rememberFragmentActivity()` returned null, disabling Enable/Use Fingerprint and causing `ClassCastException` on screens that cast context.

### Fixes applied (phase 2 — 2026-06-28)

- **`VaultUnlockFlow`** — shared premium unlock UI (navy gradient, shield badge, 6-digit PIN keypad with haptics/shake, Forgot PIN).
- **`UnlockOverlay`** — uses `VaultUnlockFlow`; title **Unlock**; `ForceSecureScreenEffect`.
- **`ForceSecureScreenEffect`** — FLAG_SECURE on unlock, detail, preview, search, backup, PIN setup.
- **Reminders bypass fixed** — linked records/pets route through `openRecord()` / `openPet()`.
- **Defense in depth** — `ModuleDetailViewModel` blocks detail when vault locked.
- **Forgot PIN** — biometric verify or explicit confirm before `PinManager.clearPinForReset()`.
- **`FriendlyAuthMessages`** — user-friendly unlock errors; no PIN logging.

See [DOCUFIND_PROTECTED_ACCESS_RULES.md](./DOCUFIND_PROTECTED_ACCESS_RULES.md).

### Fixes applied (phase 1)

- **`MainActivity` → `AppCompatActivity`** — biometrics and `BiometricPrompt` work.
- **Deferred PIN setup** — splash/onboarding → home; PIN only on first Vault open or first secure save (`SecuritySetupFlow`).
- **`UnlockOverlay`** — full-screen PIN keypad + fingerprint for home/search/recent unlock (replaces text-field dialog).
- **Vault tab** — `VaultTabScreen`: no PIN → setup; locked → `VaultScreen`; unlocked → browse.
- **Biometric setup** — Enable runs `BiometricPrompt`; hidden when unavailable; Skip / cancel is non-fatal.
- **Idle lock** — `ScreenTimeoutManager` wired via `MainActivity.onUserInteraction()`.
- **Friendly errors** — no raw “Authentication required” on unlock UI.

### Validation

- `gradle assembleDebug test`
- Manual checklist in [DOCUFIND_APP_LOCK.md](./DOCUFIND_APP_LOCK.md)

## Family & emergency form polish (2026-06-28)

### Issues found

| Area | Issue |
|------|--------|
| Family form | Cramped layout; photo picker not obvious; existing avatar not shown when editing |
| Date picker | Taps unreliable inside scroll forms |
| Relation picker | Overlay click pattern; no leading icon in field |
| Emergency form | Primary toggle lacked description |
| Capitalization | Inconsistent labels |

### Fixes applied

- **DocuFindFormScaffold** — `imePadding`, scroll-safe bottom spacing, fixed save bar above nav inset.
- **DocuFindDateField** — Box overlay click target (disabled field + full-size tap layer) for scroll forms.
- **DocuFindRelationPicker** — Leading icon, disabled field + overlay tap, `LazyColumn` bottom sheet; emergency-specific icons (Phone, LocalHospital, ContactEmergency).
- **DocuFindProfilePhotoPicker** — Card-style tappable area, 120 dp avatar, Add Photo / Change Photo, existing encrypted avatar preview on edit, permission-denied message (no crash).
- **DocuFindFamilyMemberPicker** — Same tap fix + leading Person icon.
- **FamilyFormDialog / EmergencyFormDialog** — Grouped white cards, inline validation, primary contact helper text.
- Avatar preview loaded via `SecureAttachmentStorage.decryptToCache` in family ViewModels.

### Validation

- `FormValidationTest` unit tests
- `gradle assembleDebug test`
- Manual checklist in [DOCUFIND_FAMILY_EMERGENCY.md](./DOCUFIND_FAMILY_EMERGENCY.md)

## UI reference alignment (2026-06-28)

### Mismatches found

| Area | Issue |
|------|--------|
| Theme | System dark mode changed colors away from reference |
| Launcher | Foreground did not match folder + magnifier logo |
| Splash | No entrance animation; title color inconsistent |
| Onboarding | Titles/subtitles differed; no Back button; "Skip" not "Skip Tour" |
| Bottom nav | Search tab instead of Vault |
| Vault | Screen existed but unwired; no PIN keypad; placeholder text |
| Home | Greeting-heavy header vs reference; blue-tint tiles vs white cards |
| Quick access | 11 tiles including Family/Reminders vs 3×3 reference grid |
| Biometric setup | Flag-only enable without system prompt |

### Fixes applied

- Light-only `DocuFindTheme`; safe `findActivity()` for status bar (avoids context cast crash).
- New `ic_launcher_foreground.xml` matching brand logo.
- Splash fade/scale animation; privacy string updated.
- Onboarding copy, Back, Skip Tour, layout spacing.
- `PinKeypad` shared component; vault unlock integrated.
- Vault tab in bottom nav; `VaultTabScreen` locked/unlocked states.
- Home header simplified; white card quick access 3×3.
- Biometric Enable runs `BiometricPrompt`; cancel/failure non-fatal.

### Crash root cause (launch)

- **Likely:** `Theme.kt` cast `(view.context as Activity)` when context was not an `Activity` during edge cases.
- **Fix:** `Context.findActivity()` with null-safe window access.

### Remaining limitations

- Search remains reachable via home search bar → Vault tab (when unlocked).
- Legacy `SEARCH` route kept for deep links; not in bottom bar.
- English-only strings.

## Add Document flow (2026-06-28)

### Issues found

| Area | Issue |
|------|--------|
| Date pickers | Taps on issue/expiry fields did not open picker in New Document form |
| Category dropdown | Plain text field appearance; no search for long list |
| Categories | Missing Family category; icons not visible in picker |
| Attachments | No upfront 10 MB rejection; errors silent for unsupported types |
| Save | No navigation to detail after save |
| Expiry validation | Expiry before issue not confirmed |

### Fixes applied

- **DocuFindDateField** — disabled read-only field + clickable modifier; UTC/local date conversion; Cancel does not clear value.
- **DocuFindCategoryPicker** — modal bottom sheet with icons, checkmark, search (8+ categories).
- **VaultCategory.FAMILY** added with Groups icon; 17 categories total in `VaultCategoryIcons.kt`.
- **AttachmentHelper** — `AttachmentResolveResult` with 10 MB and unsupported-type errors surfaced via snackbar.
- **AddDocumentViewModel** — expiry-before-issue confirm dialog; `SaveDocumentResult.Success(recordId)`; post-save navigation event.
- **AddSourceOptionsRow** — Scan / Gallery / File labels; Document icon for File; camera permission only on Scan tap.
- Docs: `DOCUFIND_ADD_DOCUMENT_FLOW.md`, `DOCUFIND_CATEGORY_SYSTEM.md`, `DOCUFIND_DATE_PICKER_STANDARD.md`.

### Validation

- `gradle assembleDebug test`
- Manual checklist in [DOCUFIND_ADD_DOCUMENT_FLOW.md](./DOCUFIND_ADD_DOCUMENT_FLOW.md)

## Settings & support (2026-06-28)

### Issues found

| Area | Issue |
|------|--------|
| Security | Settings → Security did not navigate reliably |
| Storage | Settings → Storage crashed on some devices |
| How to Use | Redirect to Home tab did not expand tutorial |
| Clutter | Report a Bug / Send Feedback duplicated on Settings |
| Icons | Generic icons; inconsistent title case |

### Fixes applied

- **SettingsListItem** — `Surface(onClick)` rows with chevron; distinct icons per entry.
- **Navigation** — all Settings destinations use `launchSingleTop`; dedicated `HowToUseScreen` route.
- **SecuritySettingsScreen** — Change PIN, biometric toggle, auto-lock dropdown, screenshot protection.
- **StorageScreen** — empty state; `BackupRepository` flow `.catch` + safe directory size walk.
- **HelpSupportScreen** — FAQ, Contact Support email intent, Device Info, App Version; bug/feedback moved here.
- Removed `SettingsViewModel` home-redirect hack for How to Use.
- Docs: `DOCUFIND_SETTINGS.md`, updated `DOCUFIND_SUPPORT.md`, `DOCUFIND_PRIVACY_POLICY.md`.

### Validation

- Tap every Settings item; Security, Storage, How to Use, Help & Support
- Contact Support email intent with prefilled diagnostics
- `gradle assembleDebug test`

## Reminder notifications (2026-06-28)

| Issue | Fix |
|-------|-----|
| Notifications never appeared | Reschedule active alarms on app start; cancel/reschedule on upsert; `POST_NOTIFICATIONS` + system enabled check |
| False “Notifications are off” banner | `canPostNotifications()` checks permission **and** `areNotificationsEnabled()`; permission result wired via `NotificationPermissionTracker`; refresh on `ON_RESUME` |
| Single reminder on due date only | Default offset schedule 15/7/3/1/0 days @ 9 AM via `ReminderScheduleDefaults` |
| Mark actioned left future offsets active | `completeReminderEvent()` completes entire linked event group |
| Wrong alarm cancelled for pet/medicine disable | Cancel by actual reminder IDs from DB |
| No test notification | Reminders screen **Test notification** button (5 s delay) |
| No notification actions | Mark actioned, Snooze, View record via `ReminderActionReceiver` |
| Generic notification icon | `ic_stat_docufind` monochrome app logo on **DocuFind Reminders** channel |

See [DOCUFIND_REMINDER_ENGINE.md](./DOCUFIND_REMINDER_ENGINE.md) and [DOCUFIND_NOTIFICATION_SYSTEM.md](./DOCUFIND_NOTIFICATION_SYSTEM.md).

### Validation

- Enable notifications → Test notification → confirm icon + channel
- Add insurance expiry → verify 5 scheduled reminders in Upcoming
- Mark actioned → no duplicate future notifications for same record
- `gradle assembleDebug test`

## Forms, keyboard & Settings polish (2026-06-28)

| Issue | Fix |
|-------|-----|
| White gap above keyboard on forms | `adjustNothing` + `imePadding()` on bottom save bar only; removed from scroll bodies |
| Emergency contact form unfinished | Grouped cards with section headers/icons; email field; 13-relation emergency picker |
| Phone/email awkward layout | Separate contact card; phone/email keyboards; inline validation |
| Family form polish | Section headers, photo card, phone normalization on save |
| Support forms keyboard | `ReportBugScreen` / `SendFeedbackScreen` → `DocuFindFormScaffold` |
| Emergency delete no confirm | Delete confirmation dialog |
| VM hard-coded errors | `validation_*` / `error_save_failed` string resources |

See [DOCUFIND_FORM_GUIDELINES.md](./DOCUFIND_FORM_GUIDELINES.md) and [DOCUFIND_SETTINGS.md](./DOCUFIND_SETTINGS.md).

### Validation

- Open every Settings screen (Security, Storage, Help & Support)
- Add emergency contact with keyboard open — no white space; save works
- Family member form: date picker, photo, save
- `gradle assembleDebug test`

## Database migration & data safety (2026-06-28)

| Issue | Fix |
|-------|-----|
| `fallbackToDestructiveMigration()` wiped user data on upgrade | Removed; explicit migrations 1→9 in `DocuFindMigrations.kt` |
| No migration tests | Added `DocuFindMigrationTest` with Room `MigrationTestHelper` + exported schemas |
| Migration failure crashed app | `DocuFindDatabaseFactory` + `DatabaseMigrationErrorScreen` on splash |
| No pre-migration backup | `DatabaseMigrationBackup` copies DB to `files/db_migration_backups/` |
| Missing future-proof fields | v9: `categoryMetadataJson`, `reminders.actionedAt` |
| Startup opened DB before error UI | `SplashViewModel` opens DB first; lazy Search/Reminder init; removed from `Application` |

See [DOCUFIND_MIGRATION_STRATEGY.md](./DOCUFIND_MIGRATION_STRATEGY.md), [DOCUFIND_DATA_SAFETY.md](./DOCUFIND_DATA_SAFETY.md), [DOCUFIND_DATABASE_SCHEMA.md](./DOCUFIND_DATABASE_SCHEMA.md).

### Validation

- `gradle assembleDebug test` — includes migration tests
- Manual: install old APK → add records/reminders → upgrade → vault unlock + reminders intact

## Validation

- `gradle assembleDebug test`
- Manual: clean install, PIN, skip/enable biometric, vault unlock, logcat for `AndroidRuntime`
