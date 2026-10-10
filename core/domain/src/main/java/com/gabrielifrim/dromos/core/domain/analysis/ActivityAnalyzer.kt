package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.model.TrackTotals
import com.gabrielifrim.dromos.core.domain.tracking.AutoPauseDetector
import com.gabrielifrim.dromos.core.domain.tracking.TrackAccumulator
import java.time.Duration
import java.time.Instant

/**
 * Analyse d'une sortie terminée, depuis ses points bruts.
 *
 * Elle rejoue la trace avec EXACTEMENT les filtres du suivi en direct
 * (TrackAccumulator, AutoPauseDetector) : la somme des splits retombe sur la
 * distance enregistrée, et le temps des splits exclut les mêmes arrêts.
 */
object ActivityAnalyzer {

    /** Un intervalle entre deux points retenus. */
    private class Interval(
        val start: Instant,
        val seconds: Double,
        val distance: Double,
        val gain: Double,
        val loss: Double,
        var moving: Boolean,
    )

    fun splits(
        points: List<TrackPoint>,
        type: ActivityType,
        autoPause: Boolean,
        splitMeters: Double = 1000.0,
    ): List<Split> {
        val intervals = replay(points, type, autoPause)
        if (intervals.isEmpty()) return emptyList()

        val splits = mutableListOf<Split>()
        var index = 1
        var distance = 0.0
        var seconds = 0.0
        var gain = 0.0
        var loss = 0.0
        for (interval in intervals) {
            var remainingDistance = interval.distance
            var remainingSeconds = if (interval.moving) interval.seconds else 0.0
            gain += interval.gain
            loss += interval.loss
            // Un intervalle peut franchir une borne de kilomètre : son temps est
            // réparti au prorata de la distance de part et d'autre.
            while (distance + remainingDistance >= splitMeters) {
                val needed = splitMeters - distance
                val fraction = if (remainingDistance > 0) needed / remainingDistance else 0.0
                val takenSeconds = remainingSeconds * fraction
                splits += Split(index++, splitMeters, (seconds + takenSeconds).toDuration(), gain, loss)
                remainingDistance -= needed
                remainingSeconds -= takenSeconds
                distance = 0.0; seconds = 0.0; gain = 0.0; loss = 0.0
            }
            distance += remainingDistance
            seconds += remainingSeconds
        }
        if (distance >= MIN_LAST_SPLIT_METERS) {
            splits += Split(index, distance, seconds.toDuration(), gain, loss)
        }
        return splits
    }

    /** Totaux d'une trace qui n'a pas été suivie en direct (import) : les mêmes filtres, rejoués. */
    fun totals(points: List<TrackPoint>, type: ActivityType, autoPause: Boolean): TrackTotals {
        val intervals = replay(points, type, autoPause)
        return TrackTotals(
            distanceMeters = intervals.sumOf { it.distance },
            movingDuration = intervals.filter { it.moving }.sumOf { it.seconds }.toDuration(),
            elevationGainMeters = intervals.sumOf { it.gain },
            elevationLossMeters = intervals.sumOf { it.loss },
        )
    }

    private fun replay(points: List<TrackPoint>, type: ActivityType, autoPause: Boolean): List<Interval> {
        val accumulator = TrackAccumulator()
        val detector = AutoPauseDetector(type)
        val intervals = mutableListOf<Interval>()
        var autoPaused = false
        var previous: TrackPoint? = null
        var lastDistance = 0.0
        var lastGain = 0.0
        var lastLoss = 0.0

        fun record(start: Instant, seconds: Double, moving: Boolean) {
            intervals += Interval(
                start = start,
                seconds = seconds,
                distance = accumulator.distanceMeters - lastDistance,
                gain = accumulator.elevationGainMeters - lastGain,
                loss = accumulator.elevationLossMeters - lastLoss,
                moving = moving,
            )
            lastDistance = accumulator.distanceMeters
            lastGain = accumulator.elevationGainMeters
            lastLoss = accumulator.elevationLossMeters
        }

        for (point in points) {
            if (!accumulator.add(point)) continue
            val prev = previous
            previous = point
            if (prev == null) continue
            val sameSegment = prev.segment == point.segment
            if (!sameSegment) {
                // Entre deux segments (pause manuelle) : distance du reliquat, aucun temps.
                autoPaused = false
                detector.reset()
                record(prev.timestamp, 0.0, moving = false)
                continue
            }
            val seconds = Duration.between(prev.timestamp, point.timestamp).toMillis() / 1000.0
            record(prev.timestamp, seconds, moving = !autoPaused)
            if (!autoPause) continue
            when (val decision = detector.onSpeed(point.timestamp, accumulator.instantSpeedMetersPerSecond, autoPaused)) {
                is AutoPauseDetector.Decision.Stop -> {
                    autoPaused = true
                    // Rétroactif, comme en direct : l'arrêt commence au premier instant lent.
                    intervals.filter { it.start >= decision.since }.forEach { it.moving = false }
                }
                is AutoPauseDetector.Decision.Go -> {
                    autoPaused = false
                    intervals.filter { it.start >= decision.since }.forEach { it.moving = true }
                }
                AutoPauseDetector.Decision.None -> Unit
            }
        }
        accumulator.closeSegment()
        if (accumulator.distanceMeters > lastDistance && previous != null) record(previous.timestamp, 0.0, moving = false)
        return intervals
    }

    private fun Double.toDuration(): Duration = Duration.ofMillis(Math.round(this * 1000))

    /** En deçà, le dernier tronçon est du bruit (quelques pas après l'arrivée). */
    private const val MIN_LAST_SPLIT_METERS = 20.0
}
