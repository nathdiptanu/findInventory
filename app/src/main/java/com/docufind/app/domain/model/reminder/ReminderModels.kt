package com.docufind.app.domain.model.reminder

enum class ReminderCategory(val displayName: String) {
    MEDICINE("Medicine"),
    PRESCRIPTION_REFILL("Prescription refill"),
    PRESCRIPTION_FOLLOWUP("Prescription follow-up"),
    VACCINATION("Vaccination"),
    PET_VACCINATION("Pet vaccination"),
    INSURANCE_RENEWAL("Insurance renewal"),
    VEHICLE_INSURANCE("Vehicle insurance"),
    PUC_RENEWAL("PUC renewal"),
    WARRANTY_EXPIRY("Warranty expiry"),
    PASSPORT_ID_EXPIRY("Passport / ID expiry"),
    EDUCATION("Education"),
    CUSTOM("Custom");

    companion object {
        fun fromStored(value: String?): ReminderCategory =
            entries.find { it.name == value } ?: CUSTOM
    }
}

enum class ReminderRepeatType(val displayName: String) {
    NO_REPEAT("No repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom");

    companion object {
        val selectable: List<ReminderRepeatType> = entries
        fun fromStored(value: String?): ReminderRepeatType =
            entries.find { it.name == value } ?: NO_REPEAT
    }
}

enum class ReminderImportance(val displayName: String) {
    NORMAL("Normal"),
    IMPORTANT("Important"),
    URGENT("Urgent");

    companion object {
        val selectable: List<ReminderImportance> = entries
        fun fromStored(value: String?): ReminderImportance =
            entries.find { it.name == value } ?: NORMAL
    }
}

enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    DISABLED;

    companion object {
        fun fromStored(value: String?): ReminderStatus =
            entries.find { it.name == value } ?: ACTIVE
    }
}

enum class ReminderTab(val label: String) {
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

enum class MedicineTimeSlot(val displayName: String, val minutesFromMidnight: Int) {
    MORNING("Morning", 8 * 60),
    AFTERNOON("Afternoon", 13 * 60),
    EVENING("Evening", 18 * 60),
    NIGHT("Night", 21 * 60)
}

data class ReminderListItem(
    val id: String,
    val title: String,
    val category: ReminderCategory,
    val triggerAt: Long,
    val repeatType: ReminderRepeatType,
    val importance: ReminderImportance,
    val status: ReminderStatus,
    val linkedRecordId: String?,
    val linkedPetId: String?,
    val linkedPetRecordId: String?,
    val notes: String?
)

data class CustomReminderRequest(
    val id: String? = null,
    val title: String,
    val category: ReminderCategory,
    val reminderDate: Long,
    val reminderTimeMinutes: Int,
    val repeatType: ReminderRepeatType,
    val importance: ReminderImportance,
    val notes: String?
)
