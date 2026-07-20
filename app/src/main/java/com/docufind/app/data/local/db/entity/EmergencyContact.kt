package com.docufind.app.data.local.db.entity



import androidx.room.Entity

import androidx.room.Index

import androidx.room.PrimaryKey



@Entity(

    tableName = "emergency_contacts",

    indices = [Index(value = ["name"])]

)

data class EmergencyContact(

    @PrimaryKey val id: String,

    val name: String,

    val phone: String,

    val alternatePhone: String? = null,
    val email: String? = null,
    val relationship: String? = null,

    val notes: String? = null,

    val isPrimary: Boolean = false,

    val linkedFamilyMemberId: String? = null,

    val createdAt: Long,

    val updatedAt: Long

)

