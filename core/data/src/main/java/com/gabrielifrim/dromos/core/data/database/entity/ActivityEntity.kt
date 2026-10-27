package com.gabrielifrim.dromos.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Les énumérations sont stockées par leur NOM, pas par leur ordinal : réordonner
 * une énumération du domaine ne doit jamais changer le sens d'une ligne existante.
 */
@Entity(
    tableName = "activity",
    indices = [Index("status", "started_at")],
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAtMillis: Long,
    @ColumnInfo(name = "ended_at") val endedAtMillis: Long?,
    @ColumnInfo(name = "distance_m") val distanceMeters: Double,
    @ColumnInfo(name = "moving_duration_ms") val movingDurationMillis: Long,
    @ColumnInfo(name = "elevation_gain_m") val elevationGainMeters: Double,
    @ColumnInfo(name = "elevation_loss_m") val elevationLossMeters: Double,
    val title: String?,
    val notes: String?,
    @ColumnInfo(name = "auto_pause", defaultValue = "1") val autoPause: Boolean = true,
    /** Tracé simplifié encodé (cf. PreviewCodec) ; null tant qu'il n'est pas calculé. */
    @ColumnInfo(name = "preview_polyline") val previewPolyline: String? = null,
    /** Pas du podomètre ; null sans mesure. */
    val steps: Int? = null,
)
