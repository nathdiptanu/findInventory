package com.docufind.app.data.local.db.entity



import androidx.room.Entity

import androidx.room.Index

import androidx.room.PrimaryKey



@Entity(

    tableName = "pets",

    indices = [Index(value = ["name"])]

)

data class Pet(

    @PrimaryKey val id: String,

    val name: String,

    val petType: String? = null,

    val breed: String? = null,

    val gender: String? = null,

    val birthDate: Long? = null,

    val weight: String? = null,

    val color: String? = null,

    val microchipId: String? = null,

    val vetName: String? = null,

    val vetPhone: String? = null,

    val notes: String? = null,

    val photoPath: String? = null,

    val createdAt: Long,

    val updatedAt: Long

)

