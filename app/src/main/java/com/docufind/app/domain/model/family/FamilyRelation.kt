package com.docufind.app.domain.model.family

enum class FamilyRelation(val displayName: String) {
    SELF("Self"),
    SPOUSE("Spouse"),
    SON("Son"),
    DAUGHTER("Daughter"),
    FATHER("Father"),
    MOTHER("Mother"),
    BROTHER("Brother"),
    SISTER("Sister"),
    FRIEND("Friend"),
    DOCTOR("Doctor"),
    VET("Vet"),
    NEIGHBOUR("Neighbour"),
    GRANDFATHER("Grandfather"),
    GRANDMOTHER("Grandmother"),
    GUARDIAN("Guardian"),
    OTHER("Other");

    companion object {
        val all: List<FamilyRelation> = entries

        /** Relation options shown on emergency contact forms. */
        val emergency: List<FamilyRelation> = listOf(
            SELF, SPOUSE, SON, DAUGHTER, FATHER, MOTHER, BROTHER, SISTER,
            FRIEND, DOCTOR, VET, NEIGHBOUR, OTHER
        )

        fun fromStored(value: String?): FamilyRelation =
            entries.find { it.name == value || it.displayName == value } ?: OTHER
    }
}

enum class BloodGroup(val displayName: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-"),
    UNKNOWN("Unknown");

    companion object {
        val all: List<BloodGroup> = entries
        fun fromStored(value: String?): BloodGroup =
            entries.find { it.name == value || it.displayName == value } ?: UNKNOWN
    }
}
