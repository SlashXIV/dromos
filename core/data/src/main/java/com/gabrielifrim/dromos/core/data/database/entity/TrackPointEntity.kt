package com.gabrielifrim.dromos.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_point",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Index composite : il sert à la fois la clé étrangère et la lecture ordonnée d'une trace.
    indices = [Index("activity_id", "timestamp")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "activity_id") val activityId: Long,
    @ColumnInfo(name = "timestamp") val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "altitude_m") val altitudeMeters: Double?,
    @ColumnInfo(name = "accuracy_m") val accuracyMeters: Float?,
    @ColumnInfo(name = "speed_mps") val speedMetersPerSecond: Float?,
    val segment: Int,
    @ColumnInfo(name = "bearing_deg") val bearingDegrees: Float? = null,
    @ColumnInfo(name = "baro_altitude_m") val baroAltitudeMeters: Double? = null,
)
