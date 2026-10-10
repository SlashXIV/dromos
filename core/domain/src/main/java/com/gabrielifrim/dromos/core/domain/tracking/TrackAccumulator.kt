package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.geo.GeoMath
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import kotlin.math.cos

/**
 * Cumule les mesures d'une trace, point par point : distance filtrée, dénivelé,
 * vitesse lissée.
 *
 * Toujours recalculable depuis les points bruts ([of]) : c'est ainsi qu'une
 * sortie interrompue est reconstituée, avec exactement les mêmes valeurs.
 *
 * Garde-fous avant filtrage :
 * - un point trop imprécis est écarté ;
 * - un saut à vitesse impossible (position aberrante, ancienne position en
 *   cache) n'ajoute AUCUNE distance et réinitialise le filtre : si c'est le
 *   point précédent qui était faux, la trace repart juste au lieu de rejeter
 *   indéfiniment les positions suivantes ;
 * - la distance n'est jamais comptée entre deux segments (pause manuelle).
 */
class TrackAccumulator {

    var distanceMeters: Double = 0.0
        private set

    var pointCount: Int = 0
        private set

    var lastPoint: TrackPoint? = null
        private set

    val elevationGainMeters: Double get() = elevation.gainMeters
    val elevationLossMeters: Double get() = elevation.lossMeters

    /** Altitude lissée au dernier point retenu (baromètre de préférence). */
    val altitudeMeters: Double? get() = elevation.smoothedMeters

    /** Vitesse estimée à l'instant par le filtre (réactive) : sert à la pause automatique. */
    val instantSpeedMetersPerSecond: Double get() = filter.speed

    /** Vitesse moyenne des [SPEED_WINDOW_S] dernières secondes : sert à l'allure affichée. */
    var smoothedSpeedMetersPerSecond: Double? = null
        private set

    private val filter = DistanceFilter()
    private var segmentClosed = false
    private val elevation = ElevationTracker()
    private var originLat = 0.0
    private var originLon = 0.0
    private var metersPerDegreeLon = 0.0
    private var originTime = 0L
    private val window = ArrayDeque<Pair<Double, Double>>() // (secondes, distance cumulée)

    /** @return true si le point a été retenu. */
    fun add(point: TrackPoint): Boolean {
        val accuracy = point.accuracyMeters
        if (accuracy != null && accuracy > MAX_ACCURACY_METERS) return false
        val previous = lastPoint
        if (previous == null) {
            originLat = point.latitude
            originLon = point.longitude
            metersPerDegreeLon = METERS_PER_DEGREE * cos(Math.toRadians(point.latitude))
            originTime = point.timestamp.toEpochMilli()
        }
        val newSegment = previous == null || previous.segment != point.segment
        if (newSegment) {
            closeSegment()
            filter.reset()
            if (previous != null) elevation.reset()
            window.clear()
        } else if (isJump(previous, point)) {
            filter.reset()
            window.clear()
        }

        val seconds = (point.timestamp.toEpochMilli() - originTime) / 1000.0
        val x = (point.longitude - originLon) * metersPerDegreeLon
        val y = (point.latitude - originLat) * METERS_PER_DEGREE
        distanceMeters += filter.add(
            seconds, x, y,
            accuracy = (accuracy ?: DEFAULT_ACCURACY).toDouble(),
            velocity = DistanceFilter.velocityOf(point.speedMetersPerSecond, point.bearingDegrees),
        )
        elevation.add(point.baroAltitudeMeters, point.altitudeMeters)
        updateWindow(seconds)

        lastPoint = point
        pointCount++
        segmentClosed = false
        return true
    }

    /**
     * Clôt le segment en cours (pause manuelle, fin, reconstitution) en comptant
     * le retard du filtre. Idempotent : appelé par le moteur à la pause ET
     * implicitement au segment suivant, il ne compte qu'une fois — c'est ce qui
     * garantit qu'une sortie reconstituée retombe exactement sur la même distance.
     */
    fun closeSegment() {
        if (segmentClosed) return
        distanceMeters += filter.tail()
        filter.reset()
        segmentClosed = true
    }

    private fun isJump(previous: TrackPoint, point: TrackPoint): Boolean {
        val step = GeoMath.distanceMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
        val seconds = (point.timestamp.toEpochMilli() - previous.timestamp.toEpochMilli()) / 1000.0
        return if (seconds > 0) step / seconds > MAX_SPEED_METERS_PER_SECOND else step > MAX_ACCURACY_METERS
    }

    private fun updateWindow(seconds: Double) {
        window.addLast(seconds to distanceMeters)
        while (window.size > 1 && seconds - window.first().first > SPEED_WINDOW_S) window.removeFirst()
        val (t0, d0) = window.first()
        smoothedSpeedMetersPerSecond = if (seconds - t0 >= MIN_WINDOW_S) (distanceMeters - d0) / (seconds - t0) else null
    }

    companion object {
        /** Au-delà, la position est trop floue pour mesurer une foulée. */
        const val MAX_ACCURACY_METERS = 30f

        /** 15 m/s = 54 km/h : au-delà d'un sprint, ce n'est plus une foulée. */
        const val MAX_SPEED_METERS_PER_SECOND = 15.0

        const val SPEED_WINDOW_S = 10.0
        private const val MIN_WINDOW_S = 3.0
        private const val DEFAULT_ACCURACY = 10f
        private const val METERS_PER_DEGREE = GeoMath.EARTH_RADIUS_METERS * Math.PI / 180

        /** Reconstitue une trace arrêtée (en pause ou terminée) depuis ses points bruts. */
        fun of(points: List<TrackPoint>): TrackAccumulator =
            TrackAccumulator().apply {
                points.forEach { add(it) }
                closeSegment()
            }
    }
}
