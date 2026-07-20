# DocuFind — Family, Emergency Contacts & Pets

Phase 7 delivers profile management for **family members**, **emergency contacts**, and **pets** with local-only encrypted storage.

---

## Navigation

| Entry point | Route | Screen |
|-------------|-------|--------|
| Home → Family tile | `category/family` | `FamilyListScreen` |
| Family list → member | `family/{memberId}` | `FamilyDetailScreen` |
| Settings → Family Members | `category/family` | `FamilyListScreen` |
| Settings → Emergency Contacts | `emergency_contacts` | `EmergencyListScreen` |
| Home → Pets tile | `category/pets` | `PetListScreen` |
| Pet list → pet | `pet/{petId}` | `PetDetailScreen` |

Home Quick Access counts for Family and Pets reflect **entity row counts** (`family_members`, `pets`), not vault document counts.

---

## Family Members

**Fields:** Name, Relation, DOB (optional), Blood group, Phone (optional), Email (optional), Notes, Photo (optional)

**Relations:** Self, Spouse, Son, Daughter, Father, Mother, Brother, Sister, Grandfather, Grandmother, Guardian, Other

**Blood groups:** A+, A−, B+, B−, AB+, AB−, O+, O−, Unknown

**UI:** Search bar, relation filter chips, list cards, empty state, FAB (+), detail view, edit/delete dialogs, optional encrypted photo.

**Data:** `FamilyMember` entity (Room v4), `FamilyMemberRepository`

---

## Emergency Contacts

**Fields:** Name, Relation, Phone, Alternate phone (optional), Notes, Primary contact flag

**UI:** Search bar, list with **quick call** button (ACTION_DIAL), edit/delete, primary contact badge (★), FAB (+)

Only one contact can be primary at a time; saving a new primary clears others.

**Data:** `EmergencyContact` entity (Room v4), `EmergencyContactRepository`

---

## Pets

**Pet types:** Dog, Cat, Bird, Rabbit, Fish, Other

**Pet fields:** Pet name, type, breed, gender, DOB/adoption date, weight, color, microchip ID, vet name, vet phone, notes, photo (optional)

**Pet record types:** Vaccination, Medicine, Vet Visit, Insurance, Adoption Papers, Other Document

**Vaccination fields:** Vaccine name, date given, next due date, vet/clinic, reminder enabled, upload image/PDF

**UI:** Distinct **teal accent** (`DocuFindPetTeal`) on Pets tile, FAB, and record section — friendly but professional (no childish styling).

**Pet detail** shows profile card + searchable/filterable pet records with preview/share for encrypted attachments.

**Reminders:** Vaccination records with reminder enabled create a linked `Reminder` (`petRecordId`).

**Data:** `Pet`, `PetRecord` entities (Room v4), `PetRepository`, `SecureAttachmentStorage`

---

## Database (v4)

New/extended columns:

| Table | Additions |
|-------|-----------|
| `family_members` | `dateOfBirth`, `bloodGroup` |
| `emergency_contacts` | `alternatePhone`, `isPrimary` |
| `pets` | `petType` (was `species`), `gender`, `weight`, `color`, `microchipId`, `vetName`, `vetPhone`, `photoPath` |
| `pet_records` | `vaccineName`, `nextDueDate`, `vetClinic`, `reminderEnabled`, `attachmentPath`, `attachmentMimeType`, `attachmentName` |
| `reminders` | `petRecordId` |

Development uses `fallbackToDestructiveMigration()` while schema evolves.

---

## Integration with Add Document

`VaultRecordRepository.observeFamilyMembers()` and `observePets()` feed the Add Document family/pet link dropdowns. Creating profiles in Phase 7 populates those lists.

`VaultRecord.familyMemberId` / `petId` link vault documents to profiles (unchanged from Phase 4).

---

## Key files

```
domain/model/family/FamilyRelation.kt
domain/model/pets/PetModels.kt
domain/repository/FamilyMemberRepository.kt
domain/repository/EmergencyContactRepository.kt
domain/repository/PetRepository.kt
data/repository/*RepositoryImpl.kt
data/local/storage/SecureAttachmentStorage.kt
ui/screens/family/
ui/screens/emergency/
ui/screens/pets/
ui/components/profile/ProfileListCard.kt
```
