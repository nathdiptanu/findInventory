package com.docufind.app.domain.model.search

import com.docufind.app.domain.model.module.DocuFindModule

data class SearchFilters(
    val categoryId: String? = null,
    val familyMemberId: String? = null,
    val petId: String? = null,
    val dueSoonOnly: Boolean = false,
    val expiredOnly: Boolean = false,
    val mimeType: String? = null,
    val favoriteOnly: Boolean = false
) {
    val activeCount: Int
        get() = listOfNotNull(
            categoryId?.takeIf { it.isNotBlank() },
            familyMemberId?.takeIf { it.isNotBlank() },
            petId?.takeIf { it.isNotBlank() },
            mimeType?.takeIf { it.isNotBlank() }
        ).size + listOf(dueSoonOnly, expiredOnly, favoriteOnly).count { it }
}

data class SearchResultItem(
    val recordId: String,
    val title: String,
    val categoryId: String,
    val categoryLabel: String,
    val subCategory: String?,
    val familyMemberName: String?,
    val petName: String?,
    val expiryDate: Long?,
    val isFavorite: Boolean,
    val updatedAt: Long
)

enum class SearchFilterChip(val label: String) {
    CATEGORY("Category"),
    FAMILY("Family"),
    PET("Pet"),
    DUE_SOON("Due soon"),
    EXPIRED("Expired"),
    FILE_TYPE("File type"),
    FAVORITE("Favorite")
}

object SearchMimeFilters {
    const val PDF = "application/pdf"
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"

    val options: List<Pair<String, String>> = listOf(
        PDF to "PDF",
        JPEG to "Images",
        PNG to "PNG"
    )
}

fun categoryLabel(categoryId: String): String =
    DocuFindModule.fromId(categoryId)?.title
        ?: categoryId.replace('_', ' ').replaceFirstChar { it.uppercase() }
