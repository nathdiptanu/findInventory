package com.docufind.app.domain.model

enum class QuickAccessItem(
    val id: String,
    val displayName: String
) {
    DOCUMENTS("documents", "Documents"),
    ID_CARDS("id_cards", "ID Cards"),
    CARDS("cards", "Cards"),
    MEDICAL("medical", "Medical"),
    PRESCRIPTIONS("prescriptions", "Prescriptions"),
    VACCINATION("vaccination", "Vaccination"),
    EDUCATION("education", "Education"),
    INSURANCE("insurance", "Insurance"),
    VEHICLE("vehicle", "Vehicle"),
    WARRANTY("warranty", "Warranty"),
    PETS("pets", "Pets"),
    FAMILY("family", "Family"),
    EMERGENCY("emergency", "Emergency"),
    REMINDERS("reminders", "Reminders"),
    BANKING("finance", "Banking"),
    PROPERTY("property", "Property"),
    MORE("more", "More");

    companion object {
        val homeGridOrder: List<QuickAccessItem> = listOf(
            DOCUMENTS,
            ID_CARDS,
            CARDS,
            MEDICAL,
            PRESCRIPTIONS,
            VACCINATION,
            EDUCATION,
            INSURANCE,
            VEHICLE,
            WARRANTY,
            PETS,
            FAMILY,
            EMERGENCY,
            REMINDERS,
            BANKING,
            PROPERTY,
            MORE
        )
    }
}

data class QuickAccessSummary(
    val item: QuickAccessItem,
    val itemCount: Int = 0
)

data class RecentDocument(
    val id: String,
    val title: String,
    val categoryId: String,
    val updatedAt: Long
)
