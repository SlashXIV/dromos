package com.gabrielifrim.dromos.core.domain.model

import java.time.Instant

/** Une position brute telle que livrée par la source de localisation. */
data class LocationSample(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
)
