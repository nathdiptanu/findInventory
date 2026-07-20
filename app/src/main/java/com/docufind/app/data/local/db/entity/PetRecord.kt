package com.docufind.app.data.local.db.entity



import androidx.room.Entity

import androidx.room.ForeignKey

import androidx.room.Index

import androidx.room.PrimaryKey



@Entity(

    tableName = "pet_records",

    foreignKeys = [

        ForeignKey(

            entity = Pet::class,

            parentColumns = ["id"],

            childColumns = ["petId"],

            onDelete = ForeignKey.CASCADE

        )

    ],

    indices = [Index(value = ["petId"]), Index(value = ["recordType"])]

)

data class PetRecord(

    @PrimaryKey val id: String,

    val petId: String,

    val title: String,

    val recordType: String,

    val vaccineName: String? = null,

    val nextDueDate: Long? = null,

    val vetClinic: String? = null,

    val reminderEnabled: Boolean = false,

    val attachmentPath: String? = null,

    val attachmentMimeType: String? = null,

    val attachmentName: String? = null,

    val notes: String? = null,

    val recordDate: Long? = null,

    val createdAt: Long,

    val updatedAt: Long

)

