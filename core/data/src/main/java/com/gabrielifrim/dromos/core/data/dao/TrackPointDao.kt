package com.gabrielifrim.dromos.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabrielifrim.dromos.core.data.database.entity.TrackPointEntity

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertAll(points: List<TrackPointEntity>)

    // Parcours complet de la table (l'index commence par activity_id) : quelques
    // centaines de milliers de lignes au plus, et seulement à l'import.
    @Query("SELECT EXISTS(SELECT 1 FROM track_point WHERE timestamp = :timestampMillis)")
    suspend fun existsAt(timestampMillis: Long): Boolean

    @Query("SELECT * FROM track_point WHERE activity_id = :activityId ORDER BY timestamp")
    suspend fun getForActivity(activityId: Long): List<TrackPointEntity>
}
