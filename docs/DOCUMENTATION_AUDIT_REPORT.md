# DocuFind — Documentation Audit Report

| Field | Value |
|-------|-------|
| **Purpose** | Phase 1 inventory of all project documentation before consolidation |
| **Audience** | Engineering, Product, QA, Release, AI agents |
| **Related documents** | All files under `docs/` (listed below) |
| **Last updated** | 2026-06-29 |
| **Future update rule** | Re-run this audit when doc count exceeds 12 active files or after a major release |

---

## Executive summary

DocuFind has **44 Markdown files** under `docs/` and **no root `README.md`**. Documentation grew organically across implementation phases (Phase 0–10 notes, QA passes, navigation audits, brand alignment). The content is **valuable but fragmented**: the same topics (navigation, security, colours, release QA, family forms) appear in multiple places with **inconsistent freshness** (e.g. implementation plan still describes Phase 0 as current; some docs reference Room v4/v5 while production is **v9**).

**Recommendation:** Consolidate to **~10 maintained master documents + README + optional `docs/archive/`**, preserving historical phase notes until content is migrated. **Do not delete anything until Phase 3 migration is complete.**

---

## Inventory

| Metric | Count |
|--------|------:|
| Markdown files in `docs/` | **44** |
| Root `README.md` | **0** |
| Documents with “Phase N” in title/body | **6** |
| Documents dated 2026-06-28/29 (QA sprint) | **8** |
| Estimated duplicate topic clusters | **8** |

---

## Duplicate topic map

Topics that appear in more than one document today:

| Topic | Primary sources today | Duplication severity |
|-------|---------------------|----------------------|
| **Navigation & vault gating** | `DOCUFIND_NAVIGATION`, `DOCUFIND_NAVIGATION_AUDIT`, `DOCUFIND_APP_LOCK`, `DOCUFIND_PROTECTED_ACCESS_RULES`, `DOCUFIND_ARCHITECTURE` | **High** |
| **Colours / design tokens** | `DOCUFIND_UI_GUIDELINES`, `DOCUFIND_DESIGN_SYSTEM`, `DOCUFIND_BRAND_GUIDELINES`, `DOCUFIND_UI_REFERENCE_IMPLEMENTATION` | **High** |
| **Security & encryption** | `DOCUFIND_SECURITY_MODEL`, `DOCUFIND_APP_LOCK`, `DOCUFIND_PROTECTED_ACCESS_RULES`, `DOCUFIND_DATA_SAFETY`, `DOCUFIND_STORAGE_MODEL` | **High** |
| **Release / QA** | `DOCUFIND_RELEASE_CHECKLIST`, `DOCUFIND_FINAL_QA`, `DOCUFIND_RELEASE_NOTES`, `DOCUFIND_KNOWN_BUGS_AND_FIXES` | **High** |
| **Testing** | `DOCUFIND_TESTING_STRATEGY`, `DOCUFIND_TEST_COVERAGE_REPORT`, `DOCUFIND_INTEGRATION_INTEGRITY` | **Medium** |
| **Family / emergency / pets** | `DOCUFIND_FAMILY_EMERGENCY`, `DOCUFIND_FAMILY_PETS_EMERGENCY` | **Medium** |
| **Categories & form fields** | `DOCUFIND_CATEGORY_SYSTEM`, `DOCUFIND_CATEGORY_FIELD_SPEC`, `DOCUFIND_ADD_DOCUMENT_FLOW`, `DOCUFIND_FORM_GUIDELINES` | **Medium** |
| **Reminders & notifications** | `DOCUFIND_REMINDER_ENGINE`, `DOCUFIND_NOTIFICATION_SYSTEM` | **Medium** |
| **Product vision & flows** | `DOCUFIND_PRODUCT_CONTEXT`, `DOCUFIND_USER_FLOW`, `DOCUFIND_ONBOARDING` | **Medium** |
| **Database & migration** | `DOCUFIND_DATABASE_SCHEMA`, `DOCUFIND_MIGRATION_STRATEGY`, `DOCUFIND_DATA_SAFETY` | **Medium** |
| **Home / taglines** | `DOCUFIND_HOME_SCREEN`, `DOCUFIND_TAGLINE_SYSTEM`, `DOCUFIND_UI_GUIDELINES` | **Low–Medium** |
| **Limitations vs fixed bugs** | `DOCUFIND_KNOWN_LIMITATIONS`, `DOCUFIND_KNOWN_BUGS_AND_FIXES` | **Medium** |

---

## Missing topics (not covered adequately anywhere)

| Gap | Why it matters |
|-----|----------------|
| **Root README.md** | No single entry point for humans or AI |
| **AI_CONTEXT.md** | No “read this first” agent onboarding doc |
| **FEATURE_MATRIX.md** | No master feature inventory with test/status columns |
| **Unified CHANGELOG** | Release history split across release notes, bug-fix log, QA docs |
| **Gradle / dev setup** | Scattered across support, backup, release docs; no canonical quick start |
| **Threat model (standalone)** | Security model is comprehensive but not structured as threat → control |
| **Accessibility acceptance criteria** | Mentioned as target only; no consolidated a11y checklist |
| **Documentation index** | No map of which doc owns which topic |
| **Cross-doc freshness policy** | Individual docs lack consistent “Last updated” / owner headers |

---

## Per-document audit

Classification key: **KEEP AS IS** · **MERGE INTO …** · **SPLIT** · **ARCHIVE** · **DELETE AFTER MERGING**

### Product & vision

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_PRODUCT_CONTEXT.md` | Vision, problem, scope, non-goals, design reference | Product | **High** | Overlaps `USER_FLOW`, `ONBOARDING`; “Foundation Phase (Current)” stale | Personas, journeys in depth | **MERGE INTO** `PRODUCT_BIBLE.md` (primary product SSOT) |
| `DOCUFIND_USER_FLOW.md` | First launch vs returning user; high-level nav | Product / UX | **High** | Overlaps `ONBOARDING`, `NAVIGATION` | Edge cases (migration error, forgot PIN) | **MERGE INTO** `PRODUCT_BIBLE.md` |
| `DOCUFIND_IMPLEMENTATION_PLAN.md` | Phased delivery roadmap Phases 0–5 | TPM / Engineering | **Medium** (historical) | **Outdated**: Phase 0 “current”, destructive migration, placeholder reminders listed as debt | Current phase status | **ARCHIVE** after extracting roadmap + debt into `FEATURE_MATRIX.md` and `KNOWN_LIMITATIONS.md` |
| `DOCUFIND_ONBOARDING.md` | Splash → onboarding → security → How to Use | UX / Product | **High** | PIN flow description may conflict with “PIN deferred to Vault” in product context | Profile personalization step | **MERGE INTO** `PRODUCT_BIBLE.md` (journeys) + `UI_UX_GUIDELINES.md` (screen specs) |
| `DOCUFIND_PRIVACY_POLICY.md` | In-app privacy copy (local-only) | Product / Legal | **High** | Overlaps security/privacy sections elsewhere | Store listing privacy link | **MERGE INTO** `SECURITY_AND_PRIVACY.md` (user-facing section); keep short in-app excerpt |

### Architecture & data

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_ARCHITECTURE.md` | MVVM, packages, DI, navigation overview | Engineering | **High** | Overlaps feature docs’ “Architecture” sections | Reminder engine, backup, migration pointers | **KEEP AS IS** → becomes expanded **`ARCHITECTURE.md`** SSOT |
| `DOCUFIND_DATABASE_SCHEMA.md` | Room entities, tables, v history | Engineering | **High** | Version history overlaps `MIGRATION_STRATEGY` | v9 field descriptions for all entities | **MERGE INTO** `ARCHITECTURE.md` (Database section) — single schema SSOT |
| `DOCUFIND_MIGRATION_STRATEGY.md` | Migration rules, factory, failure UI | Engineering / Release | **High** | Overlaps schema doc + `DATA_SAFETY` | — | **MERGE INTO** `ARCHITECTURE.md` (Migration section) |
| `DOCUFIND_DATA_SAFETY.md` | Upgrade guarantees, sensitive fields | Engineering / Security | **High** | Overlaps migration + security model | — | **MERGE INTO** `SECURITY_AND_PRIVACY.md` + `ARCHITECTURE.md` (split: policy vs implementation) |
| `DOCUFIND_STORAGE_MODEL.md` | Vault paths, import pipeline, file limits | Engineering | **High** | Overlaps `DOCUMENT_MODEL`, `SECURITY_MODEL` | — | **MERGE INTO** `ARCHITECTURE.md` (Storage section) |
| `DOCUFIND_DOCUMENT_MODEL.md` | VaultRecord/VaultFile, save flow | Engineering | **High** | Overlaps `ADD_DOCUMENT_FLOW`, `STORAGE_MODEL` | — | **MERGE INTO** `ARCHITECTURE.md` (Document model section) |
| `DOCUFIND_MODULES.md` | Category module registry, list/detail screens | Engineering / Product | **High** | Overlaps `CATEGORY_SYSTEM`, `MODULES` in navigation | Status per module | **MERGE INTO** `FEATURE_MATRIX.md` + `ARCHITECTURE.md` (module map) |
| `DOCUFIND_REMINDER_ENGINE.md` | Reminder categories, offsets, engine | Engineering | **High** | Says “Room v5” in places; overlaps `NOTIFICATION_SYSTEM` | actionedAt / v9 | **MERGE INTO** `ARCHITECTURE.md` (Reminder engine) |
| `DOCUFIND_NOTIFICATION_SYSTEM.md` | Channels, permissions, scheduling flow | Engineering | **High** | Overlaps reminder engine | — | **MERGE INTO** `ARCHITECTURE.md` (Notifications section) |
| `DOCUFIND_BACKUP_RESTORE.md` | Phase 10 backup/restore UX & crypto | Engineering | **High** | Phase label; overlaps security model §11–12 | — | **MERGE INTO** `ARCHITECTURE.md` (Backup/restore) |
| `DOCUFIND_SEARCH_PREVIEW_SHARE.md` | Phase 9 search, preview, share | Engineering | **High** | Phase label | — | **MERGE INTO** `ARCHITECTURE.md` (Search & preview) |

### Features & workflows (product-facing detail)

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_CATEGORY_SYSTEM.md` | Category list, icons, Quick Access | Product / UX | **High** | Overlaps `MODULES`, `CATEGORY_FIELD_SPEC` | — | **MERGE INTO** `PRODUCT_BIBLE.md` (categories) + `FEATURE_MATRIX.md` |
| `DOCUFIND_CATEGORY_FIELD_SPEC.md` | Per-category form fields | Product / Engineering | **High** | Overlaps `FORM_GUIDELINES`, `ADD_DOCUMENT_FLOW` | — | **MERGE INTO** `PRODUCT_BIBLE.md` (business rules) + `UI_UX_GUIDELINES.md` (field UX) |
| `DOCUFIND_ADD_DOCUMENT_FLOW.md` | Add screen layout, save, attachments | UX / Engineering | **High** | Overlaps form guidelines, document model | — | **MERGE INTO** `PRODUCT_BIBLE.md` (workflow) + `UI_UX_GUIDELINES.md` (screen) |
| `DOCUFIND_HOME_SCREEN.md` | Home layout, tiles, navigation | UX | **High** | Overlaps `UI_GUIDELINES`, `TAGLINE_SYSTEM` | — | **MERGE INTO** `UI_UX_GUIDELINES.md` |
| `DOCUFIND_TAGLINE_SYSTEM.md` | Rotating tagline rules | Product / UX | **Medium** | Overlaps home screen + UI guidelines | — | **MERGE INTO** `PRODUCT_BIBLE.md` (copy rules) + `UI_UX_GUIDELINES.md` (display) |
| `DOCUFIND_FAMILY_PETS_EMERGENCY.md` | Family, emergency, pets feature overview | Product | **High** | Overlaps `FAMILY_EMERGENCY`; **outdated** DB version (v4) | Align schema version | **MERGE INTO** `PRODUCT_BIBLE.md` (workflows) |
| `DOCUFIND_FAMILY_EMERGENCY.md` | Form-level UX for family/emergency | UX | **High** | Overlaps family/pets doc; form detail | Pets (in other doc) | **MERGE INTO** `UI_UX_GUIDELINES.md` (Forms: family/emergency) |
| `DOCUFIND_SETTINGS.md` | Settings list, security/storage screens | UX / Product | **High** | Overlaps support, navigation | — | **MERGE INTO** `PRODUCT_BIBLE.md` (settings map) + `UI_UX_GUIDELINES.md` |
| `DOCUFIND_SUPPORT.md` | Help & Support, diagnostics email | Product / Support | **Medium** | Overlaps settings help section | — | **MERGE INTO** `PRODUCT_BIBLE.md` (support workflow) |

### Navigation

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_NAVIGATION.md` | Routes, tabs, vault gating summary | Engineering | **High** | Overlaps audit + architecture | — | **MERGE INTO** `ARCHITECTURE.md` (Navigation) — **primary technical SSOT** |
| `DOCUFIND_NAVIGATION_AUDIT.md` | Point-in-time route matrix & fixes (2026-06-28) | QA / Engineering | **Medium** (historical) | Duplicates `NAVIGATION`; fix list in `KNOWN_BUGS_AND_FIXES` | — | **ARCHIVE** after route matrix migrated to `ARCHITECTURE.md`; fixes → `CHANGELOG.md` |

### Security

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_SECURITY_MODEL.md` | Comprehensive security architecture (22 sections) | Security / Engineering | **High** | Overlaps app lock, protected access, storage, backup | Formal threat model table | **MERGE INTO** `SECURITY_AND_PRIVACY.md` (primary SSOT) |
| `DOCUFIND_APP_LOCK.md` | Session, PIN, biometric, background lock | Security | **High** | Overlaps `PROTECTED_ACCESS_RULES`, security model §5–6 | — | **MERGE INTO** `SECURITY_AND_PRIVACY.md` |
| `DOCUFIND_PROTECTED_ACCESS_RULES.md` | Vault gate, screenshot block, forgot PIN | Security | **High** | Overlaps app lock + navigation | — | **MERGE INTO** `SECURITY_AND_PRIVACY.md` |

### UI / UX / design

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_UI_GUIDELINES.md` | Colours, typography, components, screens | UX | **High** | **High overlap** with design system, brand, reference impl | — | **MERGE INTO** `UI_UX_GUIDELINES.md` (primary SSOT) |
| `DOCUFIND_DESIGN_SYSTEM.md` | Component table, spacing, principles | UX | **High** | Duplicate palette/components with UI guidelines | — | **MERGE INTO** `UI_UX_GUIDELINES.md` |
| `DOCUFIND_BRAND_GUIDELINES.md` | Logo, brand colours, splash, voice | UX / Product | **High** | Duplicate palette with UI guidelines | — | **MERGE INTO** `UI_UX_GUIDELINES.md` (Brand chapter) |
| `DOCUFIND_UI_REFERENCE_IMPLEMENTATION.md` | Phase alignment notes vs mockup | UX | **Medium** (historical) | Duplicate palette; “changes made” is changelog-like | — | **ARCHIVE** after tokens migrated; changes → `CHANGELOG.md` |
| `DOCUFIND_FORM_GUIDELINES.md` | Keyboard, validation, pickers, photos | UX | **High** | Overlaps date picker standard, add document flow | — | **MERGE INTO** `UI_UX_GUIDELINES.md` (Forms chapter) |
| `DOCUFIND_DATE_PICKER_STANDARD.md` | DatePickerDialog standard | UX / Engineering | **Medium** | Subset of form guidelines | — | **MERGE INTO** `UI_UX_GUIDELINES.md` (Forms → Date pickers) |

### QA, testing, release

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_TESTING_STRATEGY.md` | Test layers, commands, feature-change rule | QA | **High** | Overlaps release checklist quality gates | — | **MERGE INTO** `TESTING_AND_RELEASE.md` (primary SSOT) |
| `DOCUFIND_TEST_COVERAGE_REPORT.md` | Coverage snapshot, gaps, priorities | QA | **Medium** (living) | Overlaps testing strategy | JaCoCo metrics | **MERGE INTO** `TESTING_AND_RELEASE.md` (Coverage section) — refresh per release |
| `DOCUFIND_INTEGRATION_INTEGRITY.md` | Module linkages & protecting tests | QA / Engineering | **High** | Unique integration map | — | **MERGE INTO** `TESTING_AND_RELEASE.md` (Integration integrity section) |
| `DOCUFIND_RELEASE_CHECKLIST.md` | Pre-build, gates, device flows | Release / QA | **High** | Overlaps `FINAL_QA`, testing strategy | — | **MERGE INTO** `TESTING_AND_RELEASE.md` |
| `DOCUFIND_FINAL_QA.md` | RC QA results snapshot (2026-06-29) | QA | **Medium** (historical) | Overlaps checklist + release notes; **outdated** test count (41 vs 72) | — | **ARCHIVE** after summary → `CHANGELOG.md` v1.0.0 |
| `DOCUFIND_RELEASE_NOTES.md` | Version history narrative | Release | **High** | Overlaps bug-fix log | Semver structure | **MERGE INTO** `CHANGELOG.md` (rename/reformat) |
| `DOCUFIND_KNOWN_BUGS_AND_FIXES.md` | Phase-by-phase fix journal | QA / Engineering | **Medium** (historical) | **High overlap** with release notes, limitations | — | **ARCHIVE** after fixes migrated to `CHANGELOG.md`; open items → `KNOWN_LIMITATIONS.md` |

### Limitations

| Document | Purpose | Owner | Usefulness | Duplicates / outdated | Missing | Recommended action |
|----------|---------|-------|------------|----------------------|---------|------------------|
| `DOCUFIND_KNOWN_LIMITATIONS.md` | Current debt, gaps, postponed work | Engineering / Product | **High** | Overlaps bug-fix doc “remaining limitations” | — | **KEEP AS IS** → rename to **`KNOWN_LIMITATIONS.md`** (SSOT for open debt only) |

---

## Documents recommended for merge (summary)

| Target master document | Source documents to absorb |
|------------------------|------------------------------|
| **`README.md`** (new) | Quick start distilled from architecture, gradle commands from testing/release docs |
| **`AI_CONTEXT.md`** (new) | New synthesis; links only — no duplication |
| **`PRODUCT_BIBLE.md`** (new) | `PRODUCT_CONTEXT`, `USER_FLOW`, `ONBOARDING`, `CATEGORY_SYSTEM`, `CATEGORY_FIELD_SPEC` (rules), `ADD_DOCUMENT_FLOW` (workflow), `HOME_SCREEN` (product behavior), `TAGLINE_SYSTEM`, `FAMILY_PETS_EMERGENCY`, `SETTINGS`, `SUPPORT`, `MODULES` (product view) |
| **`ARCHITECTURE.md`** | Existing `ARCHITECTURE` + `DATABASE_SCHEMA`, `MIGRATION_STRATEGY`, `STORAGE_MODEL`, `DOCUMENT_MODEL`, `REMINDER_ENGINE`, `NOTIFICATION_SYSTEM`, `BACKUP_RESTORE`, `SEARCH_PREVIEW_SHARE`, `NAVIGATION`, `MODULES` (technical view) |
| **`UI_UX_GUIDELINES.md`** (new) | `UI_GUIDELINES`, `DESIGN_SYSTEM`, `BRAND_GUIDELINES`, `FORM_GUIDELINES`, `DATE_PICKER_STANDARD`, `HOME_SCREEN` (layout), `FAMILY_EMERGENCY`, settings UI patterns |
| **`SECURITY_AND_PRIVACY.md`** (new) | `SECURITY_MODEL`, `APP_LOCK`, `PROTECTED_ACCESS_RULES`, `DATA_SAFETY`, `PRIVACY_POLICY` |
| **`TESTING_AND_RELEASE.md`** (new) | `TESTING_STRATEGY`, `TEST_COVERAGE_REPORT`, `INTEGRATION_INTEGRITY`, `RELEASE_CHECKLIST` |
| **`FEATURE_MATRIX.md`** (new) | `IMPLEMENTATION_PLAN` (status), `MODULES`, coverage gaps, roadmap |
| **`CHANGELOG.md`** (new) | `RELEASE_NOTES`, fixed items from `KNOWN_BUGS_AND_FIXES`, `FINAL_QA`, `NAVIGATION_AUDIT` |
| **`KNOWN_LIMITATIONS.md`** | Existing `KNOWN_LIMITATIONS` (dedupe against changelog fixed items) |

---

## Documents recommended for deletion (after migration only)

| Document | Risk if deleted without migration | Mitigation |
|----------|-----------------------------------|------------|
| `DOCUFIND_UI_GUIDELINES.md` | **Medium** — canonical colour tokens | Copy token table to `UI_UX_GUIDELINES.md` first |
| `DOCUFIND_DESIGN_SYSTEM.md` | **Low** | Component table is small; merge first |
| `DOCUFIND_BRAND_GUIDELINES.md` | **Medium** — logo asset paths | Preserve asset table in UI/UX Brand chapter |
| `DOCUFIND_NAVIGATION.md` | **Medium** — route reference | Merge full route table into `ARCHITECTURE.md` |
| `DOCUFIND_NAVIGATION_AUDIT.md` | **Low** (historical) | Archive or migrate matrix + changelog fixes |
| `DOCUFIND_FAMILY_EMERGENCY.md` | **Low** | Form specs live in UI/UX after merge |
| `DOCUFIND_FAMILY_PETS_EMERGENCY.md` | **Low** | Workflows in Product Bible |
| `DOCUFIND_IMPLEMENTATION_PLAN.md` | **Medium** — phase history | Extract to FEATURE_MATRIX + archive |
| `DOCUFIND_KNOWN_BUGS_AND_FIXES.md` | **High** — only record of many fixes | **Must** migrate all entries to `CHANGELOG.md` before delete |
| `DOCUFIND_FINAL_QA.md` | **Low** (snapshot) | Archive under `docs/archive/` |
| `DOCUFIND_UI_REFERENCE_IMPLEMENTATION.md` | **Low** | Archive after token migration |
| `DOCUFIND_TEST_COVERAGE_REPORT.md` | **Low** | Living section inside TESTING_AND_RELEASE |
| `DOCUFIND_INTEGRATION_INTEGRITY.md` | **Low** | Section inside TESTING_AND_RELEASE |
| `DOCUFIND_RELEASE_NOTES.md` | **Low** | Becomes CHANGELOG |
| Phase-prefixed bodies (`BACKUP_RESTORE`, `SEARCH_PREVIEW_SHARE`) | **Low** | Remove “Phase N” framing during merge |

**Never delete without archiving:** `KNOWN_BUGS_AND_FIXES`, `MIGRATION_STRATEGY`, `SECURITY_MODEL` — migrate completely first.

---

## Phase 2 — Proposed final documentation architecture

### Target structure (10 masters + README)

```
README.md                          ← NEW: project entry point
docs/
├── AI_CONTEXT.md                  ← NEW: first read for AI agents
├── PRODUCT_BIBLE.md               ← NEW: product SSOT
├── ARCHITECTURE.md                ← EXPAND existing DOCUFIND_ARCHITECTURE.md
├── UI_UX_GUIDELINES.md            ← NEW: design SSOT
├── SECURITY_AND_PRIVACY.md        ← NEW: security SSOT
├── TESTING_AND_RELEASE.md         ← NEW: QA & release SSOT
├── FEATURE_MATRIX.md              ← NEW: implementation tracker
├── CHANGELOG.md                   ← NEW: from RELEASE_NOTES
├── KNOWN_LIMITATIONS.md           ← RENAME from DOCUFIND_KNOWN_LIMITATIONS.md
└── archive/                       ← OPTIONAL: historical phase & audit docs
    ├── IMPLEMENTATION_PLAN.md
    ├── NAVIGATION_AUDIT.md
    ├── FINAL_QA.md
    ├── KNOWN_BUGS_AND_FIXES.md
    └── UI_REFERENCE_IMPLEMENTATION.md
```

### Why each document should exist

| Document | Exists because | Why not another doc? |
|----------|----------------|----------------------|
| **README.md** | Every repo needs one front door: build, structure, links | Avoids duplicating deep specs; links to masters |
| **AI_CONTEXT.md** | Agents need a ≤15 min orientation without reading 44 files | Single agent bootstrap; links outward only |
| **PRODUCT_BIBLE.md** | Product rules, journeys, categories, workflows — no code | Separates *what/why* from *how* (Architecture) |
| **ARCHITECTURE.md** | All implementation truth: DB, crypto, reminders, nav, backup | Engineers one stop; avoids product copy |
| **UI_UX_GUIDELINES.md** | One design system: brand + components + screen patterns | Eliminates 6 overlapping UX docs |
| **SECURITY_AND_PRIVACY.md** | Security is cross-cutting; users and auditors need one place | Consolidates 5 security-related docs |
| **TESTING_AND_RELEASE.md** | QA handbook: tests, gates, checklists, coverage | Release process must not scatter across 7 files |
| **FEATURE_MATRIX.md** | Tracks feature status, deps, tests — living backlog | Replaces phase plan + module list duplication |
| **CHANGELOG.md** | Professional release history | Bug-fix journal is not a changelog |
| **KNOWN_LIMITATIONS.md** | Open debt only — distinct from history | Prevents confusion with fixed bugs |

**No additional documents are necessary** if the above are maintained with strict cross-linking. Optional `docs/archive/` preserves audit trail without polluting SSOT.

### Document header standard (for Phase 3)

Every master doc should start with:

```markdown
| Purpose | … |
| Audience | … |
| Related documents | … |
| Last updated | YYYY-MM-DD |
| Update rule | When X changes, update section Y |
```

Use `[link](./OTHER.md#section)` instead of copying paragraphs.

---

## Proposed documentation index (post-consolidation)

| Topic | Single source of truth |
|-------|------------------------|
| Vision & scope | `PRODUCT_BIBLE.md` |
| User journeys | `PRODUCT_BIBLE.md` |
| Categories & field business rules | `PRODUCT_BIBLE.md` |
| Module/feature status | `FEATURE_MATRIX.md` |
| MVVM, packages, DI | `ARCHITECTURE.md` |
| Room schema & migrations | `ARCHITECTURE.md` |
| Navigation routes | `ARCHITECTURE.md` |
| Reminder engine & notifications | `ARCHITECTURE.md` |
| Backup/restore & search | `ARCHITECTURE.md` |
| Brand, colours, typography | `UI_UX_GUIDELINES.md` |
| Screen layouts & forms | `UI_UX_GUIDELINES.md` |
| PIN, biometrics, vault gate | `SECURITY_AND_PRIVACY.md` |
| Encryption & data safety | `SECURITY_AND_PRIVACY.md` |
| Privacy policy (in-app) | `SECURITY_AND_PRIVACY.md` |
| Unit/UI/migration testing | `TESTING_AND_RELEASE.md` |
| Release checklist & quality gates | `TESTING_AND_RELEASE.md` |
| Coverage & integration map | `TESTING_AND_RELEASE.md` |
| Release history | `CHANGELOG.md` |
| Open limitations & debt | `KNOWN_LIMITATIONS.md` |
| AI agent bootstrap | `AI_CONTEXT.md` |
| Build & quick start | `README.md` |

---

## Phase 3 preview (not executed in Phase 1)

When consolidation begins:

1. Create `README.md` and `AI_CONTEXT.md`.
2. Merge content in order: **SECURITY** → **ARCHITECTURE** → **UI/UX** → **PRODUCT** → **TESTING/RELEASE**.
3. Build `FEATURE_MATRIX.md` from modules + implementation plan + test coverage gaps.
4. Reformat `RELEASE_NOTES` → `CHANGELOG.md`; migrate fixed bugs from `KNOWN_BUGS_AND_FIXES`.
5. Move historical docs to `docs/archive/` with a one-line pointer in `AI_CONTEXT.md`.
6. Run **cross-reference validation**: every `docs/*.md` link resolves; no orphaned files.
7. Delete superseded files only after checklist sign-off.

---

## Cross-reference validation (current state)

| Issue | Count / note |
|-------|----------------|
| Broken relative links | Not fully audited in Phase 1 — several docs link to peers correctly (`KNOWN_BUGS` → `NAVIGATION_AUDIT`) |
| Orphan docs (nothing links to them) | ~15 phase/feature docs are leaf nodes |
| Inconsistent naming prefix | All use `DOCUFIND_` except none at root README |
| Inconsistent schema version cited | v4, v5, v7, v9 across docs — **must normalize to v9 in Phase 3** |
| Inconsistent test counts | FINAL_QA (41) vs TEST_COVERAGE (72) — normalize in TESTING master |

---

## Final recommendation — maintaining documentation going forward

1. **Cap active docs at ~10** — update masters, do not add `DOCUFIND_<FEATURE>_NOTES.md`.
2. **Feature change rule:** update `FEATURE_MATRIX.md` + relevant master + `CHANGELOG.md` (if shipped) + `TESTING_AND_RELEASE.md` (if test impact).
3. **Schema change rule:** update `ARCHITECTURE.md` database section only; never duplicate schema tables elsewhere.
4. **QA sprint rule:** append to `CHANGELOG.md` and `TESTING_AND_RELEASE.md` coverage section — do not create new `FINAL_QA_vN.md` files; use `docs/archive/` for snapshots if needed.
5. **AI agents:** read `AI_CONTEXT.md` → master for task domain → code.
6. **Quarterly doc audit:** refresh this report or a short “doc health” section in `AI_CONTEXT.md`.

---

## Phase 1 completion statement

**Phase 1 is complete.** This report was created **without modifying, merging, renaming, or deleting** any existing documentation except adding this audit file.

**Next step:** Review and approve the proposed architecture, then execute **Phase 3 consolidation** in a follow-up task.
