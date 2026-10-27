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

    @Query("SELECT * FROM activity WHERE status = :status AND type = :type ORDER BY started_at DESC")
    fun observeByStatusAndType(status: String, type: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity WHERE id = :id")
    fun observeById(id: Long): Flow<ActivityEntity?>

    @Query("SELECT id FROM activity WHERE status = :status AND preview_polyline IS NULL")
    suspend fun idsWithoutPreview(status: String): List<Long>

    @Query("UPDATE activity SET preview_polyline = :preview WHERE id = :id")
    suspend fun updatePreview(id: Long, preview: String)

    @Query("UPDATE activity SET title = :title, notes = :notes WHERE id = :id")
    suspend fun updateDetails(id: Long, title: String?, notes: String?)

    @Query("SELECT * FROM activity WHERE status = :status ORDER BY started_at DESC LIMIT 1")
    suspend fun findLatestByStatus(status: String): ActivityEntity?

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Query(
        "UPDATE activity SET distance_m = :distanceMeters, moving_duration_ms = :movingDurationMillis, " +
            "elevation_gain_m = :gain, elevation_loss_m = :loss, steps = :steps WHERE id = :id",
    )
    suspend fun updateProgress(id: Long, distanceMeters: Double, movingDurationMillis: Long, gain: Double, loss: Double, steps: Int?)

    @Query(
        "UPDATE activity SET status = :status, ended_at = :endedAtMillis, distance_m = :distanceMeters, " +
            "moving_duration_ms = :movingDurationMillis, elevation_gain_m = :gain, elevation_loss_m = :loss, " +
            "steps = :steps WHERE id = :id",
    )
    suspend fun finish(
        id: Long,
        status: String,
        endedAtMillis: Long,
        distanceMeters: Double,
        movingDurationMillis: Long,
        gain: Double,
        loss: Double,
        steps: Int?,
    )

    @Query("DELETE FROM activity WHERE id = :id")
    suspend fun delete(id: Long)
}
