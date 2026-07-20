# DocuFind — Protected Access Rules

When the vault session is locked, every path to sensitive data must authenticate first.

## Security rule

Any sensitive document, recent item, vault item, medical record, ID card, card number, password, insurance policy, or exported file **must require Vault unlock** before content is shown.

## Session check

| State | Behavior |
|-------|----------|
| Vault unlocked (recent session) | Open requested destination |
| Vault locked + PIN configured | Show **Unlock** (`VaultUnlockFlow`) |
| No PIN configured | Route to Vault tab → `SecuritySetupFlow` |

Session is cleared on app background (`AppLifecycleObserver`) and idle timeout (`ScreenTimeoutManager`).

## Protected entry points

| User action | Gate |
|-------------|------|
| Recent Items tap | `MainNavigationViewModel.openRecord()` |
| Vault browse → record | `openRecord()` |
| Category module list → record | `openRecord()` |
| Home search submit | `openSearch()` |
| Reminder → linked record | `openRecord()` |
| Reminder → linked pet | `openPet()` |
| Add flow → view saved document | `openRecord()` |
| Notification deep link | `consumePendingNavigation()` with unlock check |

**Do not** call `navController.navigate(recordDetail)` directly from UI for protected records.

## Unlock flow

```
User taps protected item
        │
        ▼
  PIN configured?
   /          \
 No            Yes
  │              │
  ▼              ▼
Vault setup   Session unlocked?
              /            \
            Yes             No
             │               │
             ▼               ▼
        Open item      UnlockOverlay / VaultScreen
                              │
                    PIN or biometric success
                              │
                              ▼
                        Open pending item
```

Shared UI: `VaultUnlockFlow` — navy gradient, shield badge, 6-digit PIN keypad (haptics + shake on error), optional biometric.

## Defense in depth

- `ModuleDetailViewModel` refuses to load detail when vault is locked; redirects via `onRequiresUnlock`.
- `SecureFileAccessManager` blocks decrypt/share/export when session is locked.
- **`AuthPurpose.DOCUMENT_EXPORT`** always requires fresh PIN/biometric via `AuthGate`, even when the vault session is already unlocked (PDF export path).
- Preview cache wiped on lock (`DocuFindApplication`).

## Screenshot blocking

`ForceSecureScreenEffect()` on:

- Unlock overlay and Vault unlock
- Search (vault browse)
- Document detail
- Document preview
- PIN setup
- Backup / restore

Default: **blocked**. Optional user setting may be added later for non-sensitive screens only.

## PIN reset (Forgot PIN)

| Biometric | Flow |
|-----------|------|
| Enabled | Biometric verify → `PinManager.clearPinForReset()` → new PIN setup |
| Disabled | Warning dialog → user confirms understanding → clear PIN → new PIN setup |

Never silently delete vault data. Never log PIN.

## Files

- `ui/navigation/MainNavigationViewModel.kt`
- `ui/components/UnlockOverlay.kt`
- `ui/components/VaultUnlockFlow.kt`
- `ui/screens/vault/VaultTabScreen.kt`
- `ui/screens/module/ModuleDetailViewModel.kt`
- `security/protection/ForceSecureScreenEffect.kt`

## Validation checklist

- [ ] Recent Item → Unlock → PIN → detail
- [ ] Vault tab → Unlock → browse
- [ ] Wrong PIN → shake + friendly message, no crash
- [ ] Cancel biometric → PIN still works
- [ ] Reminder linked record → unlock required
- [ ] App restart → vault locked
- [ ] Screenshot blocked on sensitive screens
- [ ] PDF export from unlocked vault still prompts PIN/biometric (`DOCUMENT_EXPORT`)
