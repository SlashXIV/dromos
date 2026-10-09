package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.tracking.TrackAccumulator
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ActivityAnalyzerTest {

    private val degPerMeter = 1 / 111_195.0

    /** Trajet vers le nord, une position par seconde. [speeds] : vitesse de chaque seconde. */
    private fun track(speeds: List<Double>, segmentAt: Map<Int, Int> = emptyMap(), gapSeconds: Long = 0): List<TrackPoint> {
        var north = 0.0
        var segment = 0
        var t = 0L
        return speeds.mapIndexed { i, v ->
            segmentAt[i]?.let { segment = it; t += gapSeconds; north += 300 }
            north += v
            t += 1
            TrackPoint(
                timestamp = Instant.ofEpochSecond(1_760_000_000L + t),
                latitude = 48.0 + north * degPerMeter, longitude = 2.0,
                altitudeMeters = null, accuracyMeters = 5f, speedMetersPerSecond = null, segment = segment,
            )
        }
    }

    @Test
    fun `des kilomètres complets puis un dernier tronçon partiel`() {
        val points = track(List(834) { 3.0 }) // ≈ 2,5 km à 3 m/s
        val splits = ActivityAnalyzer.splits(points, ActivityType.RUN, autoPause = false)

        assertThat(splits.map { it.index }).containsExactly(1, 2, 3).inOrder()
        assertThat(splits[0].distanceMeters).isEqualTo(1000.0)
        assertThat(splits[1].distanceMeters).isEqualTo(1000.0)
        assertThat(splits[2].distanceMeters).isWithin(15.0).of(500.0)
        // 3 m/s → 333 s/km.
        assertThat(splits[1].paceSecondsPerKm!!).isWithin(5.0).of(333.3)
    }

    @Test
    fun `la somme des splits retombe sur la distance enregistrée`() {
        val points = track(List(400) { 3.0 } + List(300) { 2.0 }, segmentAt = mapOf(400 to 1), gapSeconds = 600)
        val splits = ActivityAnalyzer.splits(points, ActivityType.RUN, autoPause = false)
        assertThat(splits.sumOf { it.distanceMeters }).isWithin(0.01).of(TrackAccumulator.of(points).distanceMeters)
    }

    @Test
    fun `une pause manuelle ne compte ni temps ni distance`() {
        // 600 s de pause et 300 m parcourus sans suivi entre les deux segments.
        val points = track(List(200) { 3.0 } + List(200) { 3.0 }, segmentAt = mapOf(200 to 1), gapSeconds = 600)
        val total = ActivityAnalyzer.splits(points, ActivityType.RUN, autoPause = false)
            .sumOf { it.movingDuration.seconds }
        assertThat(total).isIn(395L..400L)
    }

    @Test
    fun `les arrêts sont retirés quand la pause automatique était active`() {
        val speeds = List(200) { 3.0 } + List(60) { 0.0 } + List(200) { 3.0 }
        val withAuto = ActivityAnalyzer.splits(track(speeds), ActivityType.RUN, autoPause = true)
            .sumOf { it.movingDuration.seconds }
        val without = ActivityAnalyzer.splits(track(speeds), ActivityType.RUN, autoPause = false)
            .sumOf { it.movingDuration.seconds }
        assertThat(without).isIn(456L..460L)
        // L'arrêt d'une minute disparaît, au filtre près (décélération, confirmation du départ).
        assertThat(withAuto).isIn(395L..412L)
    }

    @Test
    fun `aucun point, aucun split`() {
        assertThat(ActivityAnalyzer.splits(emptyList(), ActivityType.WALK, autoPause = true)).isEmpty()
    }
}
