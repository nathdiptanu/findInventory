package com.docufind.app.data.local.db.entity



import androidx.room.Entity

import androidx.room.Index

import androidx.room.PrimaryKey



@Entity(

    tableName = "family_members",

    indices = [Index(value = ["name"])]

)

data class FamilyMember(

    @PrimaryKey val id: String,

    val name: String,

    val relationship: String,

    val dateOfBirth: Long? = null,

    val bloodGroup: String? = null,

    val phone: String? = null,

    val email: String? = null,

    val notes: String? = null,

    val avatarPath: String? = null,

    val createdAt: Long,

    val updatedAt: Long

)

