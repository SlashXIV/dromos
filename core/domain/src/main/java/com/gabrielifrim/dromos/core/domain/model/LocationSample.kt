package com.gabrielifrim.dromos.core.domain.model

import java.time.Instant

/** Une position brute telle que livrée par la source de localisation. */
data class LocationSample(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    /** Vitesse Doppler du récepteur : bien plus précise que la dérivée des positions. */
    val speedMetersPerSecond: Float? = null,
    /** Cap en degrés (0 = nord, sens horaire) ; avec la vitesse, il donne le vecteur vitesse. */
    val bearingDegrees: Float? = null,
)
