package com.gabrielifrim.dromos.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabrielifrim.dromos.core.data.database.entity.TrackPointEntity

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_point WHERE activity_id = :activityId ORDER BY timestamp")
    suspend fun getForActivity(activityId: Long): List<TrackPointEntity>
}
