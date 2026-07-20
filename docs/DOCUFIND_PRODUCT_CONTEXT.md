# DocuFind — Product Context

## Vision

DocuFind is a **private, local-only secure document vault** for Android. Users store, organize, and retrieve sensitive documents entirely on their device — no cloud, no backend, no third-party data sharing.

**Tagline:** Find it. Lock it. Trust it.

## Problem Statement

People accumulate critical documents (IDs, medical records, insurance, vehicle papers) across folders, photos, and messaging apps. These are hard to find when needed and risky to leave unprotected on a shared or unlocked phone.

## Target User

- Individuals and families who want **offline document control**
- Privacy-conscious users who refuse cloud document storage
- Users managing renewals, IDs, medical records, and household paperwork

## Core Value Propositions

| Pillar | Description |
|--------|-------------|
| **Find it** | Fast search, categories, and filters across the vault |
| **Lock it** | PIN, biometrics, auto-lock, encrypted local storage |
| **Trust it** | No network dependency; data never leaves the device |

## Product Scope (In Scope)

- Document capture/import (camera, gallery, files)
- Category-based organization (Documents, ID Cards, Medical, etc.)
- Complete Vault category metadata for Documents, ID Cards, Cards, Medical, Prescriptions, Vaccination, Education, Insurance, Vehicle, Warranty, Pets, Family, Emergency, and Others
- Secure vault with lock screen
- Reminders for renewals and recurring tasks
- Local-only Activity Insights for app usage, vault opens, search usage, stored files, category counts, reminder completion, and storage trends
- Local backup & restore
- Settings: security, storage, family/emergency contacts (future)
- Help & support, about

## Explicit Non-Goals

- No backend / server
- No cloud sync
- No AI features
- No ads
- No in-app payments
- No server-side storage
- No analytics that exfiltrate document content
- No telemetry upload; usage/activity insights stay local on the device

## Design Reference

Visual direction follows the approved brand sheet:

- **Logo:** Blue “D” with keyhole on white squircle (adaptive launcher)
- Light blue / white palette, rounded cards, accent strips on Quick Access
- Dark secure vault screen
- Bottom navigation: Home, Vault, Add, Reminders, Settings
- First launch: Splash animation → Onboarding → Profile → Home (PIN deferred to Vault)

## Foundation Phase (Current)

This repository phase establishes:

- Android project scaffold (Kotlin, Compose, MVVM)
- Navigation shell and placeholder screens
- Room schema stubs, DataStore preferences, Keystore hook
- Architecture and security documentation

Feature-complete document management, encryption at rest for files, and backup flows are planned in later phases.

## Success Metrics (Future)

- Time to find a document (< 10 seconds)
- Zero network calls for core vault operations
- Crash-free sessions during lock/unlock and navigation
- User trust: clear privacy messaging ("100% Private")

## Vault Category Product Matrix

The Add Document flow now treats the following as product-required category coverage:

| Category | Required metadata |
|----------|-------------------|
| Documents | Title, document type, issue date, optional expiry date, notes, attachments |
| ID Cards | Aadhaar, PAN, Passport, Voter ID, Driving Licence, Other ID; ID/reference, issue date, expiry date, name, attachment |
| Cards | Credit, debit, health, membership, employee; issuer, last four digits, card expiry, encrypted card number optional |
| Medical | Reports, doctor notes, allergies, diagnosis, health summary |
| Prescriptions | Doctor, patient, medicine, dosage, frequency, refill date, follow-up date |
| Vaccination | Vaccine name, date given, next due date, person/pet, reminder note |
| Education | School/college, class/grade, academic year, marksheet, certificate, activity record |
| Insurance | Provider, policy number, start date, renewal/expiry date, premium, nominee |
| Vehicle | Vehicle number, RC, insurance, PUC, service date, expiry dates |
| Warranty | Product, brand, purchase date, warranty expiry, invoice |
| Pets | Pet name, type, breed, DOB/adoption date, vaccination, medicine, vet visit, insurance, documents, photo attachment |
| Family | Name, relation, DOB, blood group, phone, email, photo attachment |
| Emergency | Name, relation, phone, alternate phone, notes |
| Others | Record type, reference, important date, details, attachments |

## Reminder Product Rules

- Default due-date schedule: 15 days before, 7 days before, 3 days before, 2 days before, 1 day before, and due date.
- Users can change the default notification time from Reminders; linked expiry/renewal/refill schedules use that local-time preference.
- Reminder sources include document/ID expiry, passport expiry, vehicle insurance, PUC, warranty expiry, insurance renewal, prescription refill/follow-up, medicine schedule, vaccination, pet vaccination, pet medicine, and custom reminders.
- Notification actions are Open, Mark Done, and Snooze.
- Completed/actioned linked reminder groups do not repeat unless the user creates a new reminder event.

## Attachment Preview Product Rules

- Every Vault attachment should expose preview metadata, Open, Share, Download, Delete, and Back.
- Images preview inline.
- PDFs preview inline when possible and fall back to a graceful placeholder when rendering is unavailable.
- Missing files never crash the app; they show the unavailable-file message.
- New uploads are copied to app-private encrypted storage.
- Family and Pet profile photos show preview after selection and support replace/remove.

## Activity Insights Product Rules

- Activity Insights is available from Settings.
- The feature is local-only and must show: "All activity insights stay on your device."
- Tracked signals are coarse usage facts only: app opens, session duration, screen visit counts, vault opens, search usage count, documents added over time, files stored count/trend, category-wise document counts, reminder creation/completion summary, and storage usage.
- Do not store document titles, notes, file names, search query text, decrypted metadata, or attachment content in activity events.
- Reports should support Daily, Weekly, and Monthly views with lightweight charts that work on small phones.
