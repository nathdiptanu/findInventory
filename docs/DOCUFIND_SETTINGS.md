# DocuFind — Settings

The **Settings** tab provides access to security, storage, family/emergency management, help, and app information.

---

## Settings list

| Item | Route | Screen |
|------|-------|--------|
| Security | `security` | `SecuritySettingsScreen` |
| Backup & Restore | `backup` | `BackupScreen` |
| Storage | `storage` | `StorageScreen` |
| Family Members | `category/family` | `FamilyListScreen` |
| Emergency Contacts | `emergency_contacts` | `EmergencyListScreen` |
| How to Use DocuFind | `how_to_use` | `HowToUseScreen` |
| Help & Support | `help_support` | `HelpSupportScreen` |
| Privacy Policy | `privacy` | `PrivacyScreen` |
| About DocuFind | `about` | `AboutScreen` |

**Not on main Settings:** Report a Bug and Send Feedback — moved under Help & Support.

---

## UI pattern

- Single card list using `SettingsListItem` (Material 3 `Surface` + `ListItem` + chevron)
- Distinct icon per row
- Title case labels from `strings.xml`
- Sub-screens use back navigation (`popBackStack`)

Navigation uses `launchSingleTop = true` from Settings to avoid duplicate entries.

---

## Security screen

| Option | Behavior |
|--------|----------|
| Change PIN | Full-screen `PinSetupScreen` overlay |
| Biometric | Toggle with system `BiometricPrompt` enrollment |
| Auto-lock timeout | 30s, 1m, 2m, 5m, or immediate on background |
| Screenshot protection | Toggle `FLAG_SECURE` via `SecurityPreferences.allowScreenshots` |

Stored in `SecurityPreferences` DataStore. Auto-lock read by `ScreenTimeoutManager`.

---

## Storage screen

Shows:

- Total app storage used
- Document count
- File count
- Last backup date
- Backup status

Empty state when no documents/files. Flow errors fall back to zeroed stats (no crash).

Data: `BackupRepository.observeStorageInfo()`.

---

## How to Use

Dedicated `HowToUseScreen` with the same step tiles as the home `HowToUseSection` (shared `HowToUseContent`).

No longer redirects to Home tab with expand flag.

---

## Help & Support

See [DOCUFIND_SUPPORT.md](./DOCUFIND_SUPPORT.md).

---

## Architecture

```
ui/screens/settings/
├── SettingsScreen.kt
├── SecuritySettingsScreen.kt / SecuritySettingsViewModel.kt
└── HowToUseScreen.kt

ui/components/
├── SettingsListItem.kt
└── HowToUseContent.kt (shared with home)
```

---

## Validation

- Tap every Settings row
- Security: change PIN, biometric, auto-lock, screenshot toggle
- Storage: open with empty and populated vault
- How to Use: full tutorial visible
- Help & Support: FAQ, bug, feedback, contact email intent (Gmail preferred)
- Emergency & Family forms: keyboard open — no white gap; save bar visible
- `gradle assembleDebug test`
