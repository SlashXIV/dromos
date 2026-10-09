package com.gabrielifrim.dromos.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabrielifrim.dromos.core.data.database.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE status = :status ORDER BY started_at DESC")
    fun observeByStatus(status: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity WHERE status = :status ORDER BY started_at DESC LIMIT 1")
    suspend fun findLatestByStatus(status: String): ActivityEntity?

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Query("UPDATE activity SET distance_m = :distanceMeters, moving_duration_ms = :movingDurationMillis WHERE id = :id")
    suspend fun updateProgress(id: Long, distanceMeters: Double, movingDurationMillis: Long)

    @Query(
        "UPDATE activity SET status = :status, ended_at = :endedAtMillis, distance_m = :distanceMeters, " +
            "moving_duration_ms = :movingDurationMillis WHERE id = :id",
    )
    suspend fun finish(id: Long, status: String, endedAtMillis: Long, distanceMeters: Double, movingDurationMillis: Long)

    @Query("DELETE FROM activity WHERE id = :id")
    suspend fun delete(id: Long)
}
