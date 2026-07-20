package com.docufind.app.domain.model

enum class VaultCategory(
    val id: String,
    val displayName: String,
    val subCategories: List<String> = emptyList()
) {
    DOCUMENTS("documents", "Documents", listOf("Personal", "Official Document", "Agreement", "Bill / Receipt", "Certificate", "Other")),
    ID_CARDS("id_cards", "ID Cards", listOf("Aadhaar", "PAN", "Passport", "Voter ID", "Driving Licence", "Other ID")),
    CARDS("cards", "Cards", listOf("Credit card", "Debit card", "Health card", "Membership card", "Employee card", "Other")),
    MEDICAL("medical", "Medical Records", listOf("Report", "Doctor Note", "Allergy", "Diagnosis", "Health Summary")),
    PRESCRIPTIONS("prescriptions", "Prescriptions", listOf("Current", "Past", "Refill", "Follow-up")),
    VACCINATION("vaccination", "Vaccination", listOf("Child", "Adult", "Travel", "Pet")),
    EDUCATION("education", "Education", listOf("Marksheet", "Certificate", "Activity Record", "Admission", "Fee Receipt")),
    INSURANCE("insurance", "Insurance", listOf("Health", "Life", "Vehicle", "Travel", "Pet", "Home", "Other")),
    VEHICLE("vehicle", "Vehicle", listOf("RC", "Insurance", "PUC", "Service", "Other")),
    WARRANTY("warranty", "Warranty", listOf("Invoice", "Warranty Card", "Service Record", "Other")),
    PETS("pets", "Pets", listOf("Vaccination", "Medicine", "Vet Visit", "Insurance", "Document")),
    FAMILY("family", "Family", listOf("Birth Certificate", "Marriage Certificate", "Family Photo ID", "Other")),
    EMERGENCY("emergency", "Emergency", listOf("Contact Info", "Medical Alert", "Insurance Card")),
    PROPERTY("property", "Property", listOf("Deed", "Rent Agreement", "Tax Receipt", "Maintenance")),
    TRAVEL("travel", "Travel", listOf("Passport", "Visa", "Ticket", "Itinerary")),
    FINANCE("finance", "Banking/Netbanking", listOf("Bank Statement", "Tax Return", "Investment", "Loan", "Netbanking")),
    OTHERS("others", "Others", listOf("Miscellaneous"));

    companion object {
        val all: List<VaultCategory> = entries
        fun fromId(id: String): VaultCategory? = entries.find { it.id == id }
    }
}

data class PendingAttachment(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPreviewPath: String? = null
)

data class FamilyMemberOption(val id: String, val name: String)
data class PetOption(val id: String, val name: String)

data class SaveDocumentRequest(
    val title: String,
    val categoryId: String,
    val subCategory: String?,
    val familyMemberId: String?,
    val petId: String?,
    val issueDate: Long?,
    val expiryDate: Long?,
    val notes: String?,
    val tags: List<String>,
    val reminderEnabled: Boolean,
    val attachments: List<PendingAttachmentEntry> = emptyList(),
    val categoryFieldValues: Map<String, String> = emptyMap()
)

data class PendingAttachmentEntry(
    val id: String,
    val uri: String,
    val attachment: PendingAttachment
)

sealed class SaveDocumentResult {
    data class Success(val recordId: String? = null) : SaveDocumentResult()
    data class Error(val message: String) : SaveDocumentResult()
}
