# DocuFind — Tagline System

Rotating motivational taglines on the Home screen header.

## Behavior

| Trigger | Action |
|---------|--------|
| App launch / `HomeViewModel` init | Pick random tagline + accent color |
| Home tab resumes (`ON_RESUME`) | Pick new random tagline + accent |

Each pick is independent — tagline text and accent color are randomized separately.

## Display rules

- Shown below **Welcome, {Name}**
- **First character**: random accent from `HomeTaglines.accentPalette` (semi-bold)
- **Remaining text**: `onSurfaceVariant` body color
- **Max 2 lines** with ellipsis overflow
- Must remain readable on light background

## Content

- **100+ taglines** in `domain/model/HomeTaglines.kt`
- Curated for privacy, local storage, organization, and peace-of-mind themes
- No user PII in taglines

## Accent palette

12 colors aligned with Quick Access / brand accents (blue, indigo, teal, green, pink, orange, purple, cyan, violet, red, deep blue, brown).

## API

```kotlin
HomeTaglines.pickRandom(): Pair<String, Color>
```

Uses `kotlin.random.Random.Default`.

## Files

| File | Role |
|------|------|
| `domain/model/HomeTaglines.kt` | Tagline list + picker |
| `ui/screens/home/HomeViewModel.kt` | `refreshGreeting()` |
| `ui/screens/home/HomeScreen.kt` | Annotated string rendering |

## Future options

- Pin favorite tagline in Settings
- Seasonal tagline sets
- Locale-specific taglines
