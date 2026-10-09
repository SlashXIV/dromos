package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.geo.GeoMath
import com.gabrielifrim.dromos.core.domain.model.TrackPoint

/**
 * Cumule la distance d'une trace, point par point.
 *
 * La distance n'est JAMAIS comptée entre deux segments : après une pause, le
 * premier point du nouveau segment repart de zéro, sinon le trajet à vol
 * d'oiseau entre l'arrêt et la reprise serait ajouté.
 *
 * Deux filtres seulement ici, le lissage fin arrivant avec le jalon
 * « Métriques fiables » :
 * - un point trop imprécis est écarté ;
 * - un saut à vitesse impossible (position aberrante, ancienne position en
 *   cache) n'ajoute AUCUNE distance, mais le point devient la nouvelle
 *   référence : si c'est le point précédent qui était faux, la trace repart
 *   juste au lieu de rejeter indéfiniment toutes les positions suivantes.
 */
class TrackAccumulator {

    var distanceMeters: Double = 0.0
        private set

    var pointCount: Int = 0
        private set

    var lastPoint: TrackPoint? = null
        private set

    /** Vitesse déduite des deux derniers points retenus ; null au premier point ou après un saut. */
    var lastSpeedMetersPerSecond: Float? = null
        private set

    /** @return true si le point a été retenu. */
    fun add(point: TrackPoint): Boolean {
        val accuracy = point.accuracyMeters
        if (accuracy != null && accuracy > MAX_ACCURACY_METERS) return false
        val previous = lastPoint
        lastSpeedMetersPerSecond = null
        if (previous != null && previous.segment == point.segment) {
            val step = GeoMath.distanceMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
            val seconds = (point.timestamp.toEpochMilli() - previous.timestamp.toEpochMilli()) / 1000.0
            val speed = if (seconds > 0) step / seconds else Double.POSITIVE_INFINITY
            if (speed <= MAX_SPEED_METERS_PER_SECOND) {
                distanceMeters += step
                lastSpeedMetersPerSecond = speed.toFloat()
            }
        }
        lastPoint = point
        pointCount++
        return true
    }

    companion object {
        /** Au-delà, la position est trop floue pour mesurer une foulée. */
        const val MAX_ACCURACY_METERS = 30f

        /** 15 m/s = 54 km/h : au-delà d'un sprint, ce n'est plus une foulée. */
        const val MAX_SPEED_METERS_PER_SECOND = 15.0

        fun of(points: List<TrackPoint>): TrackAccumulator =
            TrackAccumulator().apply { points.forEach { add(it) } }
    }
}
