package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.tracking.TrackAccumulator

/**
 * Un échantillon du profil d'une sortie, repéré par la distance parcourue.
 *
 * @property elevationMeters altitude lissée (baromètre de préférence), null sans mesure.
 * @property paceSecondsPerKm allure lissée sur quelques secondes, null à l'arrêt.
 * @property position où l'on était : le curseur des graphiques se retrouve sur la carte.
 */
data class ProfileSample(
    val distanceMeters: Double,
    val elevationMeters: Double?,
    val paceSecondsPerKm: Double?,
    val position: GeoPoint,
)

object ProfileBuilder {

    /**
     * Profil d'altitude et d'allure, rejoué avec les filtres du direct (mêmes
     * distances que les splits), puis réduit à [maxSamples] tranches de distance
     * égale : un graphique de 2 h n'a pas besoin de ses 7 200 points.
     */
    fun build(points: List<TrackPoint>, maxSamples: Int = 300): List<ProfileSample> {
        val accumulator = TrackAccumulator()
        val raw = mutableListOf<ProfileSample>()
        for (point in points) {
            if (!accumulator.add(point)) continue
            raw += ProfileSample(
                distanceMeters = accumulator.distanceMeters,
                elevationMeters = accumulator.altitudeMeters,
                paceSecondsPerKm = accumulator.smoothedSpeedMetersPerSecond
                    ?.takeIf { it >= MIN_SPEED_FOR_PACE }
                    ?.let { 1000.0 / it },
                position = GeoPoint(point.latitude, point.longitude),
            )
        }
        if (raw.size <= maxSamples) return raw
        val total = raw.last().distanceMeters
        if (total <= 0.0) return listOf(raw.last())
        // Moyenne par tranche : une pointe de bruit ne devient pas un pic du graphique.
        return raw.groupBy { ((it.distanceMeters / total) * maxSamples).toInt().coerceAtMost(maxSamples - 1) }
            .toSortedMap()
            .values
            .map { bin ->
                ProfileSample(
                    distanceMeters = bin.last().distanceMeters,
                    elevationMeters = bin.mapNotNull { it.elevationMeters }.takeIf { it.isNotEmpty() }?.average(),
                    paceSecondsPerKm = bin.mapNotNull { it.paceSecondsPerKm }.takeIf { it.isNotEmpty() }?.average(),
                    position = bin.last().position,
                )
            }
    }

    /** Comme pour l'allure affichée en direct : en dessous, on est à l'arrêt. */
    private const val MIN_SPEED_FOR_PACE = 0.5
}
