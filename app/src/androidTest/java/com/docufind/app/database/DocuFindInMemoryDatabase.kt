package com.docufind.app.database

import android.content.Context
import androidx.room.Room
import com.docufind.app.data.local.db.DocuFindDatabase

object DocuFindInMemoryDatabase {
    fun create(context: Context): DocuFindDatabase =
        Room.inMemoryDatabaseBuilder(context, DocuFindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
