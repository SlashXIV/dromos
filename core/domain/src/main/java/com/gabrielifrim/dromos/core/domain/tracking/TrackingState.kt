package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import java.time.Duration
import java.time.Instant

sealed interface TrackingState {

    data object Idle : TrackingState

    data class Active(
        val activityId: Long,
        val type: ActivityType,
        val startedAt: Instant,
        /** Pause manuelle : GPS coupé. */
        val paused: Boolean,
        /** Numéro du segment courant : +1 à chaque reprise manuelle. */
        val segment: Int,
        val distanceMeters: Double,
        /** Durée en mouvement des périodes déjà closes. */
        val movingBase: Duration,
        /** Début de la période de mouvement en cours ; null en pause (manuelle ou automatique). */
        val runningSince: Instant?,
        val pointCount: Int,
        val lastFixAt: Instant?,
        /** Vitesse lissée sur quelques secondes, pour l'allure affichée. */
        val currentSpeedMetersPerSecond: Float?,
        val accuracyMeters: Float?,
        val elevationGainMeters: Double = 0.0,
        val elevationLossMeters: Double = 0.0,
        /** Pause automatique : GPS actif, chrono arrêté jusqu'au départ suivant. */
        val autoPaused: Boolean = false,
        /** Sortie reprise après la mort du processus : l'écran le signale. */
        val recovered: Boolean = false,
        /** La source GPS a refusé de démarrer (permission retirée, GPS absent). */
        val locationError: Boolean = false,
    ) : TrackingState {

        /** Durée en mouvement à l'instant [now] : le chrono avance sans attendre un point GPS. */
        fun movingDurationAt(now: Instant): Duration =
            runningSince?.let { movingBase + Duration.between(it, now).coerceAtLeast(Duration.ZERO) }
                ?: movingBase
    }
}

private fun Duration.coerceAtLeast(min: Duration): Duration = if (this < min) min else this
