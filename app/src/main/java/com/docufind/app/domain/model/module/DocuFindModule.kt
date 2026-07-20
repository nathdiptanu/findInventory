package com.docufind.app.domain.model.module

enum class DocuFindModule(
    val id: String,
    val title: String,
    val subCategories: List<String>,
    val fieldDefinitions: List<ModuleFieldDef> = emptyList()
) {
    DOCUMENTS(
        id = "documents",
        title = "Documents",
        subCategories = listOf("Personal", "Official Document", "Agreement", "Bill / Receipt", "Certificate", "Other"),
        fieldDefinitions = listOf(
            // document_type kept for older records; new entries use Sub-category only.
            ModuleFieldDef("document_type", "Document type"),
            ModuleFieldDef("reference_number", "Reference number")
        )
    ),
    ID_CARDS(
        id = "id_cards",
        title = "ID Cards",
        subCategories = listOf("Aadhaar", "PAN", "Passport", "Voter ID", "Driving Licence", "Other ID"),
        fieldDefinitions = listOf(
            ModuleFieldDef("id_type", "ID type"),
            ModuleFieldDef("name_on_id", "Name on ID"),
            ModuleFieldDef("id_number", "ID number / reference")
        )
    ),
    CARDS(
        id = "cards",
        title = "Cards",
        subCategories = listOf("Credit card", "Debit card", "Health card", "Membership card", "Employee card", "Other"),
        fieldDefinitions = listOf(
            ModuleFieldDef("card_type", "Card type"),
            ModuleFieldDef("issuer", "Bank / issuer"),
            ModuleFieldDef("last_four", "Last 4 digits"),
            ModuleFieldDef("encrypted_card_number", "Encrypted card number")
        )
    ),
    MEDICAL(
        id = "medical",
        title = "Medical Records",
        subCategories = listOf("Report", "Doctor Note", "Allergy", "Diagnosis", "Health Summary"),
        fieldDefinitions = listOf(
            ModuleFieldDef("record_type", "Record type"),
            ModuleFieldDef("patient", "Patient / family member"),
            ModuleFieldDef("hospital", "Hospital / clinic"),
            ModuleFieldDef("doctor_name", "Doctor name"),
            ModuleFieldDef("allergies", "Allergies"),
            ModuleFieldDef("diagnosis", "Diagnosis"),
            ModuleFieldDef("health_summary", "Health summary")
        )
    ),
    PRESCRIPTIONS(
        id = "prescriptions",
        title = "Prescriptions",
        subCategories = listOf("Current", "Past", "Refill", "Follow-up"),
        fieldDefinitions = listOf(
            ModuleFieldDef("doctor_name", "Doctor name"),
            ModuleFieldDef("patient", "Patient"),
            ModuleFieldDef("medicine_name", "Medicine"),
            ModuleFieldDef("dosage", "Dosage"),
            ModuleFieldDef("frequency", "Frequency"),
            ModuleFieldDef("prescription_date", "Prescription date", ModuleFieldType.DATE),
            ModuleFieldDef("refill_date", "Refill date", ModuleFieldType.DATE),
            ModuleFieldDef("follow_up_date", "Follow-up date", ModuleFieldType.DATE)
        )
    ),
    VACCINATION(
        id = "vaccination",
        title = "Vaccination",
        subCategories = listOf("Child", "Adult", "Travel", "Pet"),
        fieldDefinitions = listOf(
            ModuleFieldDef("vaccine_name", "Vaccine name"),
            ModuleFieldDef("person_pet", "Person / Pet"),
            ModuleFieldDef("clinic", "Doctor / clinic"),
            ModuleFieldDef("reminder_note", "Reminder note"),
            ModuleFieldDef("date_given", "Date given", ModuleFieldType.DATE),
            ModuleFieldDef("next_due", "Next due date", ModuleFieldType.DATE)
        )
    ),
    EDUCATION(
        id = "education",
        title = "Education",
        subCategories = listOf("Marksheet", "Certificate", "Activity Record", "Admission", "Fee Receipt"),
        fieldDefinitions = listOf(
            ModuleFieldDef("institution", "School / College"),
            ModuleFieldDef("class_grade", "Class / Grade"),
            ModuleFieldDef("academic_year", "Academic year"),
            ModuleFieldDef("record_type", "Record type"),
            ModuleFieldDef("activity_type", "Activity type")
        )
    ),
    INSURANCE(
        id = "insurance",
        title = "Insurance",
        subCategories = listOf("Health", "Life", "Vehicle", "Travel", "Pet", "Home", "Other"),
        fieldDefinitions = listOf(
            ModuleFieldDef("insurance_type", "Insurance type"),
            ModuleFieldDef("provider", "Provider"),
            ModuleFieldDef("policy_number", "Policy number"),
            ModuleFieldDef("premium", "Premium"),
            ModuleFieldDef("nominee", "Nominee")
        )
    ),
    VEHICLE(
        id = "vehicle",
        title = "Vehicle",
        subCategories = listOf("RC", "Insurance", "PUC", "Service", "Other"),
        fieldDefinitions = listOf(
            ModuleFieldDef("vehicle_type", "Vehicle type"),
            ModuleFieldDef("vehicle_number", "Vehicle number"),
            ModuleFieldDef("rc_details", "RC details / number"),
            ModuleFieldDef("insurance_company", "Insurance company"),
            ModuleFieldDef("puc_expiry", "PUC expiry", ModuleFieldType.DATE),
            ModuleFieldDef("insurance_expiry", "Insurance expiry", ModuleFieldType.DATE),
            ModuleFieldDef("service_date", "Service date", ModuleFieldType.DATE)
        )
    ),
    WARRANTY(
        id = "warranty",
        title = "Warranty",
        subCategories = listOf("Invoice", "Warranty Card", "Service Record", "Other"),
        fieldDefinitions = listOf(
            ModuleFieldDef("product_name", "Product name"),
            ModuleFieldDef("brand", "Brand"),
            ModuleFieldDef("invoice_number", "Invoice"),
            ModuleFieldDef("purchase_date", "Purchase date", ModuleFieldType.DATE),
            ModuleFieldDef("warranty_expiry", "Warranty expiry", ModuleFieldType.DATE)
        )
    );

    val filterChips: List<String> get() = listOf("All") + subCategories

    companion object {
        val coreModules: List<DocuFindModule> = entries

        fun fromId(id: String): DocuFindModule? = entries.find { it.id == id }

        fun isSupported(categoryId: String): Boolean = fromId(categoryId) != null
    }
}

enum class ModuleFieldType { TEXT, DATE }

data class ModuleFieldDef(
    val key: String,
    val label: String,
    val type: ModuleFieldType = ModuleFieldType.TEXT
)

data class ModuleRecordItem(
    val id: String,
    val title: String,
    val subCategory: String?,
    val updatedAt: Long,
    val fileCount: Int,
    val isFavorite: Boolean
)

data class ModuleFileItem(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailPath: String?
)

data class ModuleRecordDetail(
    val id: String,
    val title: String,
    val categoryId: String,
    val moduleTitle: String,
    val subCategory: String?,
    val issueDate: Long?,
    val expiryDate: Long?,
    val notes: String?,
    val tags: List<String>,
    val metadataFields: List<ModuleFieldValue>,
    val files: List<ModuleFileItem>,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean
)

data class ModuleFieldValue(
    val label: String,
    val value: String
)

data class ModuleRecordUpdate(
    val recordId: String,
    val title: String,
    val subCategory: String?,
    val notes: String?,
    val issueDate: Long?,
    val expiryDate: Long?
)

object ModuleMetadata {
    private const val PREFIX = "meta:"

    fun encode(key: String, value: String): String = "$PREFIX$key=$value"

    fun parse(tags: List<String>): Map<String, String> = tags
        .filter { it.startsWith(PREFIX) }
        .mapNotNull { tag ->
            val body = tag.removePrefix(PREFIX)
            val idx = body.indexOf('=')
            if (idx <= 0) null else body.substring(0, idx) to body.substring(idx + 1)
        }
        .toMap()

    fun userTags(tags: List<String>): List<String> = tags.filter { !it.startsWith(PREFIX) }

    fun buildFieldValues(module: DocuFindModule, tags: List<String>): List<ModuleFieldValue> {
        val meta = parse(tags)
        return module.fieldDefinitions.mapNotNull { def ->
            meta[def.key]?.takeIf { it.isNotBlank() }?.let { value ->
                ModuleFieldValue(def.label, formatValue(value, def.type))
            }
        }
    }

    private fun formatValue(raw: String, type: ModuleFieldType): String = when (type) {
        ModuleFieldType.TEXT -> raw
        ModuleFieldType.DATE -> raw.toLongOrNull()?.let { formatEpoch(it) } ?: raw
    }

    private fun formatEpoch(epoch: Long): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
        return java.time.Instant.ofEpochMilli(epoch)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }
}
