# DocuFind Navigation

## Bottom navigation

Five destinations in the main tab bar:

| Tab | Route | Screen |
|-----|-------|--------|
| Home | `home` | `HomeScreen` |
| Vault | `vault` | Locked unlock / unlocked browse |
| Add | `add` | `AddScreen` |
| Reminders | `reminders` | `RemindersScreen` |
| Settings | `settings` | `SettingsScreen` |

The bottom bar is hidden for pushed screens (category lists, record detail, support, etc.).

## Secondary routes

| Route | Screen | Entry points |
|-------|--------|--------------|
| `search` | `SearchScreen` | Home search bar; Vault tab when unlocked |
| `category/{categoryId}` | Module list / hub / family / pets | Quick Access tiles |
| `record/{recordId}` | Record detail | Recent items, search, module lists (via unlock gate) |
| `emergency_contacts` | Emergency list | Quick Access Emergency; Settings |
| `privacy`, `backup`, `storage`, `security`, etc. | Settings sub-screens | Settings |

## Home navigation

| Action | Behavior |
|--------|----------|
| Quick Access tile | Category module, family, pets, emergency, reminders tab, or More hub |
| Recent item | `MainNavigationViewModel.openRecord()` → unlock overlay if needed → `record/{id}` |
| Search submit | `openSearch()` → Search screen (optional query prefilled) |
| Bell icon | Reminders tab |

## Vault gating

Protected navigation (`openRecord`, `openSearch`) checks PIN configuration and vault session. Unlock overlay or Vault tab PIN setup handles authentication before routing.

## Root graph (pre-main)

```
splash → onboarding → main
```

PIN/biometric setup is deferred to first Vault open or first secure document save.

## File map

| File | Role |
|------|------|
| `ui/navigation/DocuFindRoutes.kt` | Route constants |
| `ui/navigation/MainScreen.kt` | Bottom bar + NavHost |
| `ui/navigation/MainNavigationViewModel.kt` | Unlock gate + pending navigation |
| `ui/components/DocuFindBottomBar.kt` | Tab bar with center Add |
