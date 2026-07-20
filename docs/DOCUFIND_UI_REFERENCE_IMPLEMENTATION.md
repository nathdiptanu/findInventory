# DocuFind — UI Reference Implementation

**Design source of truth:** The uploaded DocuFind reference image (light blue / white / deep blue, folder + magnifier logo, rounded white cards, vault gradient screen).

This document records how the app aligns with that reference.

## Palette (exact tokens)

| Token | Hex | Usage |
|-------|-----|--------|
| `DocuFindBlue` | `#42A5F5` | Primary actions, icons |
| `DocuFindNavy` / `DocuFindDeepBlue` | `#0D47A1` / `#1A237E` | Headlines, app name |
| `DocuFindBlueSurface` | `#F5FAFF` | Page background |
| `DocuFindWhite` | `#FFFFFF` | Cards |
| `DocuFindCardBorder` | `#E0E7EF` | Card borders |
| `DocuFindVaultBackground` | `#0A2E5C` | Vault locked gradient |
| `DocuFindTeal` | `#00897B` | Privacy badge accent |

**Theme:** Light mode only for the main flow (`DocuFindTheme(darkTheme = false)`).

## Logo & launcher

- In-app: `DocuFindLogo` — blue rounded square, folder, lock badge, navy magnifier circle.
- Launcher: `ic_launcher_foreground.xml` — adaptive icon foreground matching the same motif; background `#E8F4FD`.

## Screen alignment

| Screen | Reference match |
|--------|-----------------|
| Splash | Centered logo, DocuFind title (deep blue), tagline, privacy pill, fade/scale animation |
| Onboarding 1–3 | Updated copy, illustrations, dots, Back (page 2+), Skip Tour, Next / Get Started |
| PIN setup | 6-digit dots, centered keypad, light background |
| Biometric (optional) | After PIN only; skippable; real biometric prompt on Enable |
| Home | DocuFind header, search bar, 3×3 quick access white cards, recent items, bottom nav |
| Vault tab | Locked → gradient vault screen + PIN + fingerprint; unlocked → search/browse |
| Bottom nav | Home · Vault · Add (FAB) · Reminders · Settings |

## Icon rules

- Single style: Material filled icons tinted `primary` blue on white cards.
- Quick access: Description, Badge, CreditCard, MedicalServices, School, Shield, DirectionsCar, Pets, MoreHoriz.

## Flow

1. Splash → 2. Onboarding → 3. Create PIN → 4. Optional biometric → 5. Main (Home).
5. Vault tab shows locked screen until PIN/biometric unlock.
6. Biometric is **never** prompted before PIN setup completes.

## Changes made (reference alignment)

- Forced light theme; fixed status-bar setup without unsafe `Activity` cast crash.
- Rebuilt launcher foreground vector to match logo.
- Splash animation + privacy copy update.
- Onboarding titles/subtitles and navigation controls per reference.
- Shared `PinKeypad` / `PinDotIndicator` for PIN setup and vault unlock.
- Vault screen: deep blue gradient, shield icon, PIN keypad, Unlock + fingerprint.
- Bottom nav: Search tab replaced with Vault tab.
- Home quick access: 3×3 grid (Documents … More) on white `DocuFindCard` tiles.
- Biometric setup uses system prompt; cancel does not crash.

## Validation

See `DOCUFIND_USER_FLOW.md` and `DOCUFIND_KNOWN_BUGS_AND_FIXES.md`.
