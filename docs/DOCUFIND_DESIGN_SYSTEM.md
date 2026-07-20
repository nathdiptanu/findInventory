# DocuFind — Design System

**Source of truth:** Uploaded DocuFind reference design image.

## Principles

- Calm, premium, uncluttered layout.
- Light blue page fill, white cards, deep blue typography.
- Consistent blue line-style icons (Material, primary tint).
- Generous spacing; no overlapping CTAs on onboarding or PIN screens.

## Components

| Component | File | Notes |
|-----------|------|-------|
| `DocuFindLogo` | `DocuFindComponents.kt` | Folder + lock + magnifier |
| `DocuFindCard` | `DocuFindComponents.kt` | 16dp radius, 1dp border, 2dp elevation |
| `DocuFindPrimaryButton` | `DocuFindComponents.kt` | 52dp height, 16dp radius |
| `DocuFindSearchBar` | `DocuFindComponents.kt` | White field, blue focus border |
| `PinKeypad` / `PinDotIndicator` | `PinKeypad.kt` | PIN setup + vault unlock |
| `QuickAccessGrid` | `QuickAccessGrid.kt` | 3-column white tiles |
| `DocuFindBottomBar` | `DocuFindBottomBar.kt` | 5-item nav with center Add FAB |

## Typography

Material 3 defaults; headlines use `secondary` (deep blue), body uses `onSurfaceVariant`.

## Spacing

- Screen horizontal padding: 16–24dp
- Card internal padding: 14–16dp
- Quick access grid gap: 12dp
- List FAB clearance: 88dp bottom padding

## Do not

- Enable dark theme in main flow until explicitly requested.
- Mix filled and outlined icon families on the same screen.
- Use hard-coded fixed heights that clip keypad or onboarding buttons on small screens.
