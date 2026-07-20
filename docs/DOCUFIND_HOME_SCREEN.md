# DocuFind Home Screen

The Home dashboard is the primary entry point after onboarding. Security setup (PIN) is deferred until the user opens Vault or saves a document.

## Layout (top to bottom)

1. **Header**
   - App name: **DocuFind**
   - Greeting: **Welcome, {Name}** (or **Welcome** if no profile name)
   - Rotating tagline below (random accent on first character, max 2 lines)
   - Reminders bell icon (top right) → Reminders tab

2. **Search bar**
   - Placeholder: *Search documents…*
   - Submit opens Search screen (empty query allowed)
   - Routes through vault unlock when session is locked

3. **Quick Access**
   - Section title: *Quick Access*
   - 3-column grid, 17 tiles:
     Documents, ID Cards, Cards, Medical, Prescriptions, Vaccination, Education, Insurance, Vehicle, Warranty, Pets, Family, Emergency, Reminders, Banking, Property, More
   - White rounded cards, 4dp left accent bar per category
   - Icon in tinted circle; label single-line with ellipsis (`softWrap = false`)
   - Fixed tile height 96dp — no mid-word wrapping (e.g. Reminders)

4. **Recent Items**
   - Up to 5 documents by `updatedAt`
   - Each row: icon, generic protected title, category, protected-vault label, lock indicator
   - Empty: *No documents yet. Add your first document securely.*
   - Tap → `openRecord()` with vault gate (never direct to detail)
   - Record titles, file names, notes, and timestamps are intentionally hidden on Home before unlock

5. **How to Use DocuFind**
   - Collapsible card on Home; full page in Settings
   - **Expanded** on first Home visit (`howToUseIntroSeen` false) or after security setup
   - **Collapsed** after user dismisses; reopen from Settings → How to Use
   - Vertical step list (not horizontal scroll): Add → Organize → Secure → Remind → Find → Access

## Rotating tagline

See [DOCUFIND_TAGLINE_SYSTEM.md](./DOCUFIND_TAGLINE_SYSTEM.md).

- 100+ taglines in `HomeTaglines.kt`
- Random pick on app launch and when Home resumes
- First character uses random accent from palette; remainder uses `onSurfaceVariant`

## Tile accent colors

| Category | Accent |
|----------|--------|
| Documents | Blue |
| ID Cards | Indigo |
| Cards | Cyan |
| Medical | Pink |
| Prescriptions | Purple |
| Vaccination | Teal |
| Education | Violet |
| Insurance | Green |
| Vehicle | Orange |
| Warranty | Amber |
| Pets | Brown/Orange |
| Family | Purple |
| Emergency | Red |
| Reminders | Orange |
| Banking | Deep blue |
| Property | Brown |
| More | Grey |

## Navigation from Home

| Action | Destination |
|--------|-------------|
| Quick Access (document categories) | `category/{id}` module list or placeholder |
| Banking | `category/finance` |
| Property | `category/property` |
| Family | Family list |
| Pets | Pets list |
| Emergency | Emergency contacts |
| Reminders | Reminders tab |
| More | Module hub |
| Recent item | Record detail via unlock gate |
| Search submit | Search screen |
| Bell icon | Reminders tab |

## Layout notes

- Home uses a single `LazyColumn` inside Main `Scaffold` — no nested Scaffold (avoids double bottom padding / white gap).
- Bottom inset comes from Main navigation bar only.

## Data sources

| UI section | Source |
|------------|--------|
| Quick Access counts | `DocumentRepository.observeQuickAccessSummaries()` |
| Recent Items | `DocumentRepository.observeRecentDocuments(limit = 5)` |
| Tagline / greeting | `HomeViewModel` + `HomeTaglines` + `PreferencesRepository` |
| How to Use state | `PreferencesRepository` + `HomeViewModel` |

## Files

| File | Role |
|------|------|
| `ui/screens/home/HomeScreen.kt` | Layout |
| `ui/screens/home/HomeViewModel.kt` | Flows, tagline, How to Use persistence |
| `ui/components/QuickAccessGrid.kt` | Tile grid + accents |
| `ui/components/RecentItemsSection.kt` | Recent list + empty state |
| `ui/components/HowToUseSection.kt` | Collapsible guide |
| `ui/components/HowToUseContent.kt` | Vertical step list |
| `domain/model/QuickAccessItem.kt` | Tile definitions |
| `domain/model/HomeTaglines.kt` | Tagline pool |

## Polish fixes (2026-06-28)

- Removed nested `Scaffold` on Home — fixes bottom white gap.
- Quick Access labels: `softWrap = false`, ellipsis, 11sp label.
- Added Banking + Property tiles.
- How to Use: vertical steps, no horizontal LazyRow overlap.
- Header shows DocuFind + Welcome + rotating tagline.
- Recent items show category icon and label.

## Privacy polish (2026-07-17)

- Recent Items now avoid showing document titles or timestamps on Home.
- A lock affordance and "Protected by DocuFind Vault" label make it clear that tapping routes through secure unlock.
- This protects the Home dashboard from casual exposure while keeping category-level context useful.
