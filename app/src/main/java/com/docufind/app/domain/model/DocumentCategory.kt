package com.docufind.app.domain.model

enum class DocumentCategory(
    val id: String,
    val displayName: String,
    val iconKey: String
) {
    DOCUMENTS("documents", "Documents", "folder"),
    ID_CARDS("id_cards", "ID Cards", "badge"),
    CARDS("cards", "Cards", "credit_card"),
    MEDICAL("medical", "Medical", "medical"),
    EDUCATION("education", "Education", "school"),
    INSURANCE("insurance", "Insurance", "shield"),
    VEHICLE("vehicle", "Vehicle", "directions_car"),
    PETS("pets", "Pets", "pets"),
    MORE("more", "More", "more_horiz");

    companion object {
        fun fromId(id: String): DocumentCategory? = entries.find { it.id == id }
    }
}
