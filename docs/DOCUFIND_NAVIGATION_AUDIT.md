# DocuFind — Navigation Audit

**Date:** 2026-06-28  
**Scope:** Full main-graph navigation, vault gating, empty-database safety, and defensive UI states.

## Summary

All required screens are registered in `MainScreen.kt` NavHost. Quick Access tiles, Settings entries, bottom tabs, and vault-gated flows route to valid composables. Crashes from swapped search-bar parameters, invalid record IDs, unsafe activity casts, and missing unlock/search pending state were fixed.

## Route matrix

| Screen | Route | Registered | Entry points | Empty DB safe |
|--------|-------|------------|--------------|---------------|
| Home | `home` | Yes | Bottom tab, start destination | Yes |
| Search | `search` | Yes | Home search bar (after unlock) | Yes |
| Add Document | `add` | Yes | Bottom tab, module FAB | Yes |
| Documents | `category/documents` | Yes | Quick Access | Yes (`ModuleEmptyState`) |
| ID Cards | `category/id_cards` | Yes | Quick Access | Yes |
| Cards | `category/cards` | Yes | Quick Access | Yes |
| Medical | `category/medical` | Yes | Quick Access | Yes |
| Prescriptions | `category/prescriptions` | Yes | Quick Access | Yes |
| Vaccination | `category/vaccination` | Yes | Quick Access | Yes |
| Education | `category/education` | Yes | Quick Access | Yes |
| Insurance | `category/insurance` | Yes | Quick Access | Yes |
| Vehicle | `category/vehicle` | Yes | Quick Access | Yes |
| Warranty | `category/warranty` | Yes | Quick Access | Yes |
| Pets | `category/pets` | Yes | Quick Access | Yes |
| Family | `category/family` | Yes | Quick Access, Settings | Yes |
| Emergency Contacts | `emergency_contacts` | Yes | Quick Access, Settings | Yes |
| Reminders | `reminders` | Yes | Bottom tab, Quick Access, bell | Yes |
| Vault / Unlock | `vault` | Yes | Bottom tab | Yes (PIN setup / lock / browse) |
| Settings | `settings` | Yes | Bottom tab | Yes |
| Security | `security` | Yes | Settings | Yes |
| Storage | `storage` | Yes | Settings | Yes (empty vault message) |
| Backup & Restore | `backup` | Yes | Settings | Yes |
| How to Use | `how_to_use` | Yes | Settings | Yes |
| Help & Support | `help_support` | Yes | Settings | Yes |
| Privacy Policy | `privacy` | Yes | Settings, Help | Yes |
| About | `about` | Yes | Settings | Yes |
| More hub | `category/more` | Yes | Quick Access | Yes |
| Record detail | `record/{recordId}` | Yes | Search, recent, modules | Yes (not-found state) |

## Quick Access tile routing

| Tile | Route / action |
|------|----------------|
| Documents … Warranty | `category/{id}` → `ModuleListScreen` when `DocuFindModule.isSupported(id)` |
| Pets | `category/pets` → `PetListScreen` |
| Family | `category/family` → `FamilyListScreen` |
| Emergency | `emergency_contacts` |
| Reminders | `reminders` tab (`navigateToTab`) |
| More | `category/more` → `ModuleHubScreen` |

Unknown category IDs fall through to `CategoryPlaceholderScreen` (no crash).

## Vault gating

| State | Vault tab behavior |
|-------|-------------------|
| No PIN | `SecuritySetupFlow` |
| Locked | `VaultScreen` (PIN keypad + optional biometric) |
| Unlocked | `SearchScreen` (vault browse) |

Protected navigation via `MainNavigationViewModel`:

- `openRecord(id)` — blank ID ignored; no PIN → Vault setup; locked → `UnlockOverlay`; unlocked → `record/{id}`
- `openSearch(query)` — blank query allowed after unlock; locked → overlay then Search; no PIN → Vault setup

## Issues fixed in this audit

| Issue | Root cause | Fix |
|-------|------------|-----|
| Vault screen does not open | Tab wired but lock/setup states unclear | `VaultTabScreen` three-state branch |
| Recent Items crash | Direct nav bypassing vault gate / blank IDs | `openRecord()` gate; blank ID guard on Home |
| Search crash | Swapped `DocuFindSearchBar` args | Correct `query` / `onQueryChange` order |
| Settings sub-screens crash | Missing routes / unreliable clicks | All routes in NavHost; `SettingsListItem` Surface onClick |
| How to Use crash | LazyVerticalGrid inside home scroll | Dedicated `how_to_use` route + horizontal home section |
| Date picker crash | Picker inside scroll without tap target | `DocuFindDateField` Box overlay pattern |
| Search blank after unlock | `pendingSearchAfterUnlock` only set for non-blank query | `pendingOpenSearch` flag for empty query |
| Record detail infinite loading | Missing / blank record ID | `notFound` + `DocuFindAsyncContent` |
| Activity cast crash | `context as FragmentActivity` | `rememberFragmentActivity()` |

## Defensive UI states

Shared component: `DocuFindAsyncContent` (`Loading` / `Empty` / `Error + Retry` / `Content`).

| Screen | Loading | Empty | Error + Retry |
|--------|---------|-------|---------------|
| Module lists | Flow initial | `ModuleEmptyState` / filter no-match | — |
| Search | Debounce indicator (`isSearching`) | Empty hint / no results | — |
| Record detail | `DocuFindAsyncContent` | Record not found | Snackbar on action errors |
| Storage | Flow | Zero records/files card | Flow `.catch` in ViewModel |
| Home recent | Flow | `RecentItemsSection` empty card | — |
| Vault | Auth in progress | N/A (lock UI) | Inline auth error |

## Search checklist

- [x] Works with zero records (empty hint, no crash)
- [x] Typing debounced (300 ms), no crash
- [x] Empty query submit opens Search (after unlock)
- [x] Back from Search route (`popBackStack` + system back)
- [x] Filter chips with empty family/pet lists

## Manual validation checklist

Run on device (e.g. CPH2707, Android 15):

1. **Bottom tabs** — Home, Vault (all three states), Add, Reminders, Settings
2. **Quick Access** — Tap all 15 tiles; confirm list or special screen opens
3. **Settings** — Security, Storage, Backup, How to Use, Help, Privacy, About
4. **Search** — Empty DB, type query, clear query, submit empty, back
5. **Recent items** — Empty state; with records tap item (unlock if needed)
6. **Add Document** — Open form, tap issue/expiry date pickers, save
7. **Vault** — No PIN setup; lock/unlock; browse when unlocked
8. **Back** — System back from every pushed screen
9. **Restart** — Force-stop and reopen; session lock behaves correctly
10. **Logcat** — Filter `AndroidRuntime`; expect no `FATAL`

## Build validation

```powershell
Set-Location C:\Users\MSUSERSL123\Documents\DocuFind
.\gradle-dist\gradle-8.9\bin\gradle.bat assembleDebug test
```

Expected: `BUILD SUCCESSFUL`

## File map

| File | Role |
|------|------|
| `ui/navigation/DocuFindRoutes.kt` | Route constants |
| `ui/navigation/MainScreen.kt` | NavHost + bottom bar |
| `ui/navigation/MainNavigationViewModel.kt` | Vault gate + pending navigation |
| `ui/screens/vault/VaultScreen.kt` | `VaultTabScreen`, lock UI |
| `ui/components/DocuFindAsyncContent.kt` | Shared async UI states |
| `ui/components/DocuFindDateField.kt` | Safe date picker |
| `ui/components/DocuFindComponents.kt` | `DocuFindSearchBar` |

See also: [DOCUFIND_NAVIGATION.md](./DOCUFIND_NAVIGATION.md), [DOCUFIND_KNOWN_BUGS_AND_FIXES.md](./DOCUFIND_KNOWN_BUGS_AND_FIXES.md)
