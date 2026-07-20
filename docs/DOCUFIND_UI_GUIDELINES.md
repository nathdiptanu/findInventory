# DocuFind — UI Guidelines

**Phase 1 status:** Complete — theme, splash, onboarding, first-time education aligned with design reference.

Design reference: DocuFind mockup (light blue / white, clean cards, rounded tiles).

## Brand

| Element | Guideline |
|---------|-----------|
| App name | **DocuFind** — bold, deep blue headline |
| Tagline | *Find it. Lock it. Trust it.* |
| Logo | Blue **D with keyhole** on white squircle — `ic_launcher_foreground.xml` |
| Privacy badge | “100% private. Everything stays on your device.” on splash animation |

## Color Palette

Defined in `ui/theme/Color.kt`:

| Token | Hex | Role |
|-------|-----|------|
| DocuFindBlue | `#42A5F5` | **Primary** — light blue actions, icons |
| DocuFindBlueDark | `#1565C0` | Primary dark variant |
| DocuFindNavy / DocuFindDeepBlue | `#0D47A1` / `#1A237E` | **Secondary** — deep blue headlines |
| DocuFindTeal | `#00897B` | **Accent** — privacy badge, secure step highlight |
| DocuFindTealLight | `#B2DFDB` | Accent container |
| DocuFindBlueLight | `#E3F2FD` | Icon circles, badges |
| DocuFindBlueSurface | `#F5FAFF` | **Background** — very light blue page fill |
| DocuFindWhite | `#FFFFFF` | Card surfaces |
| DocuFindCardBorder | `#E0E7EF` | Soft card border |
| DocuFindVaultBackground | `#0A2E5C` | Vault locked screen |
| DocuFindTextPrimary | `#1A237E` | Body headlines |
| DocuFindTextSecondary | `#607D8B` | Captions, counts |

Material 3 mapping: `primary` = light blue, `secondary` = deep blue, `tertiary` = teal, `background` = light blue surface, `surface` = white cards.

## Typography

- **Font family:** System sans-serif (Roboto on Android)
- **Display / headlines:** Bold, deep blue (`secondary` color on onboarding)
- **Body:** Regular, secondary gray for supporting copy
- **Labels:** Medium weight for buttons and chips

See `DocuFindTypography` in `ui/theme/Type.kt`.

## Shape & Elevation

- **Corner radius:** 16dp cards/tiles; 24dp pills/badges
- **Cards:** White surface, 1dp soft border (`DocuFindCardBorder`), 2dp elevation — use `DocuFindCard`
- **Buttons:** Full-width primary buttons with 16dp radius on onboarding/setup
- **Search bar:** White fill, soft border, blue focus ring

## Components

### DocuFindLogo

Layered composition: folder (primary blue) + lock (top-right) + search circle (navy, bottom-right).

### Quick Access tiles

- White `DocuFindCard` with soft border (1dp `DocuFindCardBorder`, 2dp elevation)
- **4dp left accent bar** — category-specific color (see `DOCUFIND_HOME_SCREEN.md`)
- Icon: 20dp inside 34dp tinted circle; Material filled icons
- Label: `labelSmall` 11sp, semibold, **`softWrap = false`**, single line, ellipsis
- Tile height: fixed 96dp — prevents mid-word breaks (e.g. “Reminders”)
- 3-column grid, 10dp gutters; 17 tiles including Banking and Property

### How to Use

- Collapsible card on Home; **vertical step list** when expanded (not horizontal scroll)
- Step order: Add → Organize → Secure → Remind → Find → Access
- Each step: icon in tinted square + title + short description
- Tap header row to expand/collapse; chevron up/down
- Settings opens full-page version with same vertical content

Standard container for tiles, settings groups, and How to Use section. White background, border + shadow.

### Category tile (Home grid)

- 3-column grid
- Icon (primary blue) + category name + item count
- `DocuFindCard` with 16dp corners

### Vault category forms

- Every date input must use `DocuFindDateField`; do not expose manual date typing by default.
- Primary date labels should match the selected category: warranty expiry, card expiry, renewal/expiry date, next due date, refill/follow-up date.
- Dropdowns and subcategory selectors use `DocuFindOptionPicker` bottom sheets with icons where available.
- Dynamic category metadata lives under one `Category details` card; avoid nested cards inside the form.
- Attachment controls remain visible for all Vault categories, including Family/Pets photo records and Emergency photo records where applicable.
- Keep forms keyboard-safe with `imePadding`, 48dp minimum touch targets, and single-column layout on small screens.

### How to Use (Home)

- Collapsible card at bottom of Home scroll
- Header: title + expand/collapse chevron (entire row tappable)
- Expanded: vertical list with dividers — icon + title + description per step
- Teal accent on Secure step icon
- Expanded first time only after setup; collapsed on subsequent launches

### Home header

- **DocuFind** (primary, titleMedium)
- **Welcome, {Name}** (headlineMedium, bold)
- Rotating tagline — first letter accent color, max 2 lines (see `DOCUFIND_TAGLINE_SYSTEM.md`)

### Bottom navigation

Four destinations with icons + labels:

1. Home (house)
2. Vault (shield)
3. Reminders (clock)
4. Settings (gear)

### Vault locked screen

- Full-screen dark navy background
- Large lock/shield icon (white)
- Primary "Unlock" + secondary fingerprint action

### Onboarding

- Horizontal pager, 3 pages (see `DOCUFIND_ONBOARDING.md`)
- Large circular illustration area (160dp) on light blue background
- Page illustrations: folder+shield (welcome), folder-copy (organize), search+notifications (reminders)
- Dot indicators, deep blue titles
- Full-width primary CTA (52dp height): "Next" / "Get Started"
- Top-right "Skip"

### Settings list

- Grouped list inside `DocuFindCard`
- Leading blue icon + title per row
- **How to Use DocuFind** opens Home with section expanded

### Reminders

- Filter chips: All, Upcoming, Completed
- List cards with title, date, frequency

## Spacing

- Screen horizontal padding: **16–24dp**
- Grid gap: **12dp**
- Section vertical spacing: **16dp**

## Iconography

Material Icons Extended — simple filled icons:

- Onboarding: shield, folder_copy, notifications_active
- How to Use: add, folder, shield, search, description, notifications
- Categories: folder, badge, credit_card, medical_services, etc.

## Accessibility (Target)

- Minimum touch target 48dp
- Content descriptions on interactive icons
- Sufficient contrast on vault screen (white on navy)

## Dark Mode

Theme includes a dark scheme anchored to vault navy tones. Primary screens optimize for **light mode** per design reference.

## Do Not Use

- Cloud upload iconography implying sync
- Third-party brand colors
- Heavy gradients or skeuomorphic textures
- Ad placeholders or paywall patterns

## Universal Preview UX - 2026-07-02

- Always provide a top-left Back action on attachment preview screens.
- Show metadata before the preview body: file name, size, type, created date, and category.
- Use inline image preview for images and inline PDF preview when rendering succeeds.
- If preview is not available, show exactly: "Preview unavailable. You can open or share this file."
- If the file is missing, show exactly: "File unavailable. The original file may have been moved or deleted."
- Keep Open, Share, Download, and Delete actions reachable without crowding the toolbar on small phones.
- Family and Pet photo forms must show the selected/current image, support replace/remove, and treat camera/gallery cancellation as a no-op.

## Activity Insights UX - 2026-07-03

- Entry point: Settings -> Activity Insights.
- Show the privacy message exactly: "All activity insights stay on your device."
- Provide Daily, Weekly, and Monthly filter chips near the top.
- Use lightweight horizontal bar charts; do not add heavy chart libraries.
- Required cards: summary chips, Activity, Documents added, Files stored, Category-wise documents, Reminder completion.
- Keep labels single-line where possible, allow ellipsis, and avoid wide fixed charts that can overflow on small phones.
- Empty states must be calm: zero-value charts and "No documents stored yet." instead of blank panels.
