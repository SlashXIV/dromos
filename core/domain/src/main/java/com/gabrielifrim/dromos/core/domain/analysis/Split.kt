package com.gabrielifrim.dromos.core.domain.analysis

import java.time.Duration

/**
 * Un tronçon de la sortie, normalement d'un kilomètre ; le dernier est partiel.
 * L'allure se rapporte à la distance réelle du tronçon.
 */
data class Split(
    /** 1 pour le premier kilomètre. */
    val index: Int,
    val distanceMeters: Double,
    val movingDuration: Duration,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
) {
    /** Secondes au kilomètre ; null pour un tronçon trop court pour être parlant. */
    val paceSecondsPerKm: Double?
        get() = if (distanceMeters < MIN_METERS || movingDuration.isZero) null
        else movingDuration.toMillis() / 1000.0 / distanceMeters * 1000

    private companion object {
        const val MIN_METERS = 50.0
    }
}
