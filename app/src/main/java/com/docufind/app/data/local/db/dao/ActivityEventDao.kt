package com.docufind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.docufind.app.data.local.db.entity.ActivityEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ActivityEvent)

    @Query("SELECT * FROM activity_events WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun observeSince(since: Long): Flow<List<ActivityEvent>>

    @Query("DELETE FROM activity_events WHERE timestamp < :before")
    suspend fun deleteBefore(before: Long)
}
