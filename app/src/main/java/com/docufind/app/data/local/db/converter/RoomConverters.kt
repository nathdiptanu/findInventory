package com.docufind.app.data.local.db.converter

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(TAG_DELIMITER)

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(TAG_DELIMITER).map { it.trim() }

    companion object {
        private const val TAG_DELIMITER = "\u001F"
    }
}
