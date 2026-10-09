package com.gabrielifrim.dromos.core.domain.model

import java.time.Instant

/**
 * Un point GPS enregistré, BRUT : le filtrage se fait au calcul, jamais au
 * stockage. Une amélioration future de l'algorithme pourra ainsi recalculer
 * les anciennes sorties.
 *
 * [segment] augmente à chaque reprise après une pause manuelle : la distance
 * n'est jamais comptée entre deux segments.
 */
data class TrackPoint(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val segment: Int,
    val bearingDegrees: Float? = null,
    /** Altitude barométrique relative au moment du point ; null sans baromètre. */
    val baroAltitudeMeters: Double? = null,
)
