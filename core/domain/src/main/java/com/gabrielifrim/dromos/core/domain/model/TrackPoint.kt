package com.gabrielifrim.dromos.core.domain.model

import java.time.Instant

/**
 * Un point GPS enregistré.
 *
 * [segment] augmente à chaque reprise après une pause : la distance n'est jamais
 * comptée entre deux segments, sinon une pause café ajouterait le trajet à vol
 * d'oiseau entre le point d'arrêt et le point de reprise.
 */
data class TrackPoint(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val segment: Int,
)
