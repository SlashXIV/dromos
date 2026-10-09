package com.gabrielifrim.dromos.core.domain.model

import java.time.Duration

/** Totaux d'une sortie, tels qu'enregistrés avec elle. */
data class TrackTotals(
    val distanceMeters: Double,
    val movingDuration: Duration,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
)
