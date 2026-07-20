package com.docufind.app.domain.model

enum class CategoryFieldKind {
    TEXT,
    DATE,
    CHOICE,
    SENSITIVE,
    PASSWORD,
    MULTILINE
}

data class CategoryFieldDef(
    val key: String,
    val label: String,
    val kind: CategoryFieldKind = CategoryFieldKind.TEXT,
    val choices: List<String> = emptyList(),
    val optional: Boolean = true,
    val multiline: Boolean = false
)

object CategoryFieldRegistry {

    fun fieldsFor(categoryId: String): List<CategoryFieldDef> = when (categoryId) {
        "documents" -> documentsFields
        "id_cards" -> idCardsFields
        "education" -> educationFields
        "cards" -> cardsFields
        "finance" -> bankingFields
        "property" -> propertyFields
        "insurance" -> insuranceFields
        "vehicle" -> vehicleFields
        "pets" -> petsFields
        "medical" -> medicalFields
        "prescriptions" -> prescriptionFields
        "warranty" -> warrantyFields
        "vaccination" -> vaccinationFields
        "family" -> familyFields
        "emergency" -> emergencyFields
        "others" -> othersFields
        else -> emptyList()
    }

    fun discretionWarningKey(categoryId: String): String? = when (categoryId) {
        "cards" -> "cards_sensitive_warning"
        "finance" -> "banking_discretion_warning"
        else -> null
    }

    fun usesIssueDate(categoryId: String): Boolean =
        categoryId in setOf(
            "documents", "id_cards", "insurance", "vehicle", "warranty", "vaccination", "prescriptions",
            "finance", "property", "pets", "medical"
        )

    fun usesExpiryDate(categoryId: String): Boolean =
        categoryId in setOf(
            "documents", "id_cards", "cards", "insurance", "vehicle", "warranty", "vaccination",
            "prescriptions", "finance", "property", "pets"
        )

    fun showReminderByDefault(categoryId: String): Boolean =
        categoryId in setOf(
            "documents", "id_cards", "cards", "insurance", "vehicle", "warranty", "vaccination",
            "prescriptions", "pets", "finance", "property"
        )

    fun issueDateLabel(categoryId: String): String = when (categoryId) {
        "insurance" -> "Start date"
        "vehicle" -> "Service date"
        "warranty" -> "Purchase date"
        "vaccination" -> "Date given"
        "prescriptions" -> "Prescription date"
        "finance" -> "Opening date"
        "property" -> "Purchase date"
        "pets" -> "Visit / record date"
        "medical" -> "Record date"
        else -> "Issue date"
    }

    fun expiryDateLabel(categoryId: String): String = when (categoryId) {
        "cards" -> "Card expiry"
        "insurance" -> "Renewal / expiry date"
        "vehicle" -> "Insurance / RC expiry"
        "warranty" -> "Warranty expiry"
        "vaccination" -> "Next due date"
        "prescriptions" -> "Refill / follow-up date"
        "finance" -> "Maturity date"
        "property" -> "Agreement renewal date"
        "pets" -> "Next vaccination / due date"
        else -> "Expiry date"
    }

    private val documentsFields = listOf(
        // Type comes from Sub-category picker only (no duplicate dropdown).
        CategoryFieldDef("reference_number", "Reference number (optional)")
    )

    private val idCardsFields = listOf(
        // ID type comes from Sub-category picker only.
        CategoryFieldDef("name_on_id", "Name on ID"),
        CategoryFieldDef("id_number", "ID number / reference", CategoryFieldKind.SENSITIVE)
    )

    private val educationFields = listOf(
        CategoryFieldDef("institution", "School / College"),
        CategoryFieldDef("class_grade", "Class / Grade"),
        CategoryFieldDef("academic_year", "Academic year"),
        // Record type comes from Sub-category picker only.
        CategoryFieldDef("activity_type", "Activity type (optional)")
    )

    private val cardsFields = listOf(
        // Card type comes from Sub-category picker only.
        CategoryFieldDef("issuer", "Bank / issuer"),
        CategoryFieldDef("last_four", "Last 4 digits"),
        CategoryFieldDef("encrypted_card_number", "Full card number (optional, stored encrypted)", CategoryFieldKind.SENSITIVE)
    )

    private val bankingFields = listOf(
        CategoryFieldDef("account_display_name", "Account display name"),
        CategoryFieldDef("bank_name", "Bank name"),
        CategoryFieldDef("account_holder_name", "Account holder name"),
        CategoryFieldDef(
            "account_type",
            "Account type",
            CategoryFieldKind.CHOICE,
            listOf("Savings", "Current", "Fixed Deposit", "Recurring Deposit", "Loan", "Credit", "Netbanking", "Other")
        ),
        CategoryFieldDef("account_number", "Account number", CategoryFieldKind.SENSITIVE),
        CategoryFieldDef("ifsc", "IFSC code (optional)"),
        CategoryFieldDef("micr", "MICR code (optional)"),
        CategoryFieldDef("branch_name", "Branch name (optional)"),
        CategoryFieldDef("branch_address", "Branch address (optional)", CategoryFieldKind.MULTILINE, multiline = true),
        CategoryFieldDef("nominee_name", "Nominee name (optional)"),
        CategoryFieldDef("nominee_relationship", "Nominee relationship (optional)"),
        CategoryFieldDef("username", "Customer ID (optional)", CategoryFieldKind.SENSITIVE),
        CategoryFieldDef("password", "Netbanking password (optional)", CategoryFieldKind.PASSWORD),
        CategoryFieldDef("url", "Netbanking URL (optional)"),
        CategoryFieldDef("contact_details", "Contact details (optional)", CategoryFieldKind.MULTILINE, multiline = true)
        // Notes use the shared Notes field on the Add screen — not repeated here.
    )

    private val propertyFields = listOf(
        CategoryFieldDef("property_title", "Property title (optional)"),
        CategoryFieldDef(
            "property_type",
            "Property type",
            CategoryFieldKind.CHOICE,
            listOf("Residential", "Commercial", "Land", "Agricultural", "Rental", "Other")
        ),
        CategoryFieldDef(
            "ownership",
            "Ownership",
            CategoryFieldKind.CHOICE,
            listOf("Owned", "Rented", "Leased", "Inherited", "Under construction", "Other")
        ),
        CategoryFieldDef("owners", "Owner(s)"),
        CategoryFieldDef("co_owners", "Co-owner(s) (optional)"),
        CategoryFieldDef("address", "Address", CategoryFieldKind.MULTILINE, multiline = true),
        CategoryFieldDef("city", "City"),
        CategoryFieldDef("state", "State"),
        CategoryFieldDef("postal_code", "Postal code (optional)"),
        CategoryFieldDef("registration_number", "Registration number (optional)"),
        CategoryFieldDef("purchase_value", "Purchase value (optional)"),
        CategoryFieldDef("estimated_value", "Estimated value (optional)"),
        CategoryFieldDef("builder", "Builder (optional)"),
        CategoryFieldDef("seller", "Seller (optional)"),
        CategoryFieldDef("nominee_notes", "Nominee notes (optional)", CategoryFieldKind.MULTILINE, multiline = true),
        CategoryFieldDef("loan_reference", "Loan reference (optional)"),
        CategoryFieldDef("insurance", "Insurance (optional)"),
        CategoryFieldDef("tax_due_date", "Tax due date (optional)", CategoryFieldKind.DATE),
        CategoryFieldDef("maintenance_due_date", "Maintenance due date (optional)", CategoryFieldKind.DATE)
        // Notes use the shared Notes field on the Add screen — not repeated here.
    )

    private val insuranceFields = listOf(
        // Insurance type comes from Sub-category picker only.
        CategoryFieldDef("provider", "Provider"),
        CategoryFieldDef("policy_number", "Policy number", CategoryFieldKind.SENSITIVE),
        CategoryFieldDef("premium", "Premium"),
        CategoryFieldDef("nominee", "Nominee")
    )

    private val vehicleFields = listOf(
        CategoryFieldDef(
            "vehicle_type",
            "Vehicle type",
            CategoryFieldKind.CHOICE,
            listOf("2 Wheeler", "4 Wheeler", "Other")
        ),
        CategoryFieldDef("vehicle_number", "Vehicle number"),
        CategoryFieldDef("rc_details", "RC details / number"),
        CategoryFieldDef("insurance_company", "Insurance company"),
        // Primary dates use Issue/Expiry above the form — keep only extra vehicle dates.
        CategoryFieldDef("puc_expiry", "PUC expiry (optional)", CategoryFieldKind.DATE)
    )

    private val petsFields = listOf(
        CategoryFieldDef(
            "pet_type",
            "Pet type",
            CategoryFieldKind.CHOICE,
            listOf("Dog", "Cat", "Bird", "Rabbit", "Fish", "Other")
        ),
        CategoryFieldDef("pet_name_field", "Pet name"),
        CategoryFieldDef("breed", "Breed"),
        CategoryFieldDef("dob_adoption", "DOB / adoption date", CategoryFieldKind.DATE),
        // Pet record type comes from Sub-category picker only.
        CategoryFieldDef("vet_name", "Vet name"),
        CategoryFieldDef("vet_phone", "Vet phone"),
        CategoryFieldDef("vaccination_name", "Vaccination name"),
        CategoryFieldDef("medicine", "Medicine"),
        CategoryFieldDef("insurance_provider", "Insurance provider"),
        CategoryFieldDef("medical_notes", "Medical notes", CategoryFieldKind.MULTILINE, multiline = true)
    )

    private val medicalFields = listOf(
        // Record type comes from Sub-category picker only.
        CategoryFieldDef("patient", "Patient / family member"),
        CategoryFieldDef("hospital", "Hospital / clinic"),
        CategoryFieldDef("doctor_name", "Doctor name"),
        CategoryFieldDef("allergies", "Allergies", CategoryFieldKind.MULTILINE, multiline = true),
        CategoryFieldDef("diagnosis", "Diagnosis", CategoryFieldKind.MULTILINE, multiline = true),
        CategoryFieldDef("health_summary", "Health summary", CategoryFieldKind.MULTILINE, multiline = true)
    )

    private val prescriptionFields = listOf(
        CategoryFieldDef("doctor_name", "Doctor name"),
        CategoryFieldDef("patient", "Patient"),
        // Prescription / refill dates use Issue / Expiry fields above.
        CategoryFieldDef("medicine_name", "Medicine"),
        CategoryFieldDef("dosage", "Dosage"),
        CategoryFieldDef("frequency", "Frequency")
    )

    private val warrantyFields = listOf(
        CategoryFieldDef("product_name", "Product name"),
        CategoryFieldDef("brand", "Brand"),
        // Purchase / warranty expiry use Issue / Expiry fields above.
        CategoryFieldDef("invoice_number", "Invoice")
    )

    private val vaccinationFields = listOf(
        CategoryFieldDef("vaccine_name", "Vaccine name"),
        CategoryFieldDef("person_pet", "Person / Pet"),
        // Date given / next due use Issue / Expiry fields above.
        CategoryFieldDef("clinic", "Doctor / clinic"),
        CategoryFieldDef("reminder_note", "Reminder note (optional)")
    )

    private val familyFields = listOf(
        CategoryFieldDef("name", "Name"),
        CategoryFieldDef(
            "relation",
            "Relation",
            CategoryFieldKind.CHOICE,
            listOf("Self", "Spouse", "Child", "Parent", "Sibling", "Grandparent", "Other")
        ),
        CategoryFieldDef("dob", "DOB", CategoryFieldKind.DATE),
        CategoryFieldDef(
            "blood_group",
            "Blood group",
            CategoryFieldKind.CHOICE,
            listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")
        ),
        CategoryFieldDef("phone", "Phone"),
        CategoryFieldDef("email", "Email"),
        CategoryFieldDef("photo_note", "Photo / document attachment note", CategoryFieldKind.MULTILINE, multiline = true)
    )

    private val emergencyFields = listOf(
        CategoryFieldDef("name", "Name"),
        CategoryFieldDef("relation", "Relation"),
        CategoryFieldDef("phone", "Phone"),
        CategoryFieldDef("alternate_phone", "Alternate phone"),
        CategoryFieldDef("emergency_notes", "Notes", CategoryFieldKind.MULTILINE, multiline = true)
    )

    private val othersFields = listOf(
        CategoryFieldDef("record_type", "Record type"),
        CategoryFieldDef("reference_number", "Reference number (optional)"),
        CategoryFieldDef("important_date", "Important date", CategoryFieldKind.DATE),
        CategoryFieldDef("details", "Details", CategoryFieldKind.MULTILINE, multiline = true)
    )
}
