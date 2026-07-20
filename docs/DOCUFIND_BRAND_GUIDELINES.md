# DocuFind — Brand Guidelines

**Approved logo:** Blue **“D” with keyhole** (reference #14).  
**App label:** DocuFind  
**Tagline:** Find it. Lock it. Trust it.

## Logo

| Asset | File | Usage |
|-------|------|--------|
| Shared logo | `res/drawable/ic_docufind_mark.xml` | **Canonical** blue D + keyhole — Home, splash, About, watermark, notifications |
| Adaptive foreground | `res/drawable/ic_launcher_foreground.xml` | `<inset>` 18% all sides around **same** `ic_docufind_mark` (equal X/Y; no scale transforms) |
| Monochrome layer | `res/drawable/ic_launcher_monochrome.xml` | Matching silhouette |
| Adaptive background | `@color/ic_launcher_background` | White `#FFFFFF` |
| Adaptive icons | `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | foreground + background + monochrome |
| In-app composable | `DocuFindLogo` / `DocuFindBrandMark` | Uses `ic_docufind_mark` with `ContentScale.Fit` |

### Icon rules

- Launcher and in-app must use **`ic_docufind_mark` only** — do not invent a second glyph.
- Prefer uniform `<inset>` percentage padding over nested `scaleX`/`scaleY` groups.
- Never use `ContentScale.FillBounds` on the brand mark.

## Color palette (logo-aligned)

| Role | Hex | Use |
|------|-----|-----|
| Primary blue | `#1A73E8` | D body, primary actions |
| Sky highlight | `#4FC3F7` | Splash, accents |
| Deep blue | `#0D47A1` / `#1A237E` | Headlines, vault |
| White | `#FFFFFF` | Cards, icon background |
| Light surface | `#F5FAFF` | Page background |

Quick Access accent strips use the same family (see `QuickAccessGrid.kt`).

## Typography

- **DocuFind** — bold, deep blue, title case.
- **Taglines** — sentence case, medium weight, gray-blue body.
- **Privacy line** — “100% private. Everything stays on your device.”

## Splash animation

Concept (2.2s, lightweight Compose):

1. Vault door appears  
2. Key slides into keyhole  
3. Door opens / fades  
4. D logo + “DocuFind”  
5. Tagline fades in  

Implementation: `SplashUnlockAnimation.kt`

## Voice & copy

| Context | Approved copy |
|---------|----------------|
| Splash tagline | Find it. Lock it. Trust it. |
| Privacy | 100% private. Everything stays on your device. |
| Onboarding 1 | Welcome to DocuFind |
| Onboarding 3 | Find Quickly. Stay Reminded. |
| Profile | Let's Personalize DocuFind |
| Home greeting | Welcome, {Name} |

**Avoid:** “Search fast, share securely, access anywhere” (retired).

## Do not

- Reintroduce folder/magnifier-only logo without keyhole D.
- Force PIN/biometric at splash or onboarding.
- Use cloud/login branding — local-only product.

See also: [DOCUFIND_UI_GUIDELINES.md](./DOCUFIND_UI_GUIDELINES.md), [DOCUFIND_PRODUCT_CONTEXT.md](./DOCUFIND_PRODUCT_CONTEXT.md)
