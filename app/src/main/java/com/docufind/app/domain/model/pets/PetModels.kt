package com.docufind.app.domain.model.pets

enum class PetType(val displayName: String) {
    DOG("Dog"),
    CAT("Cat"),
    BIRD("Bird"),
    RABBIT("Rabbit"),
    FISH("Fish"),
    OTHER("Other");

    companion object {
        val all: List<PetType> = entries
        fun fromStored(value: String?): PetType =
            entries.find { it.name == value || it.displayName == value } ?: OTHER
    }
}

enum class PetGender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    UNKNOWN("Unknown");

    companion object {
        val all: List<PetGender> = entries
        fun fromStored(value: String?): PetGender =
            entries.find { it.name == value || it.displayName == value } ?: UNKNOWN
    }
}

enum class PetRecordType(val displayName: String) {
    VACCINATION("Vaccination"),
    MEDICINE("Medicine"),
    VET_VISIT("Vet Visit"),
    INSURANCE("Insurance"),
    ADOPTION_PAPERS("Adoption Papers"),
    OTHER("Other Document");

    companion object {
        val all: List<PetRecordType> = entries
        val filterChips: List<String> = listOf("All") + entries.map { it.displayName }
        fun fromStored(value: String?): PetRecordType =
            entries.find { it.name == value || it.displayName == value } ?: OTHER
    }
}
