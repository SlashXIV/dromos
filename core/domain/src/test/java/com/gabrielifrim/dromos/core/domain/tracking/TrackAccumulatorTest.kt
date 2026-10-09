package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class TrackAccumulatorTest {

    // 0,001° de latitude ≈ 111,2 m ; un point par minute ≈ 1,9 m/s, une allure de marche.
    private fun point(i: Int, lat: Double, segment: Int = 0, accuracy: Float? = 5f) = TrackPoint(
        timestamp = Instant.ofEpochSecond(i * 60L),
        latitude = lat, longitude = 2.0,
        altitudeMeters = null, accuracyMeters = accuracy, speedMetersPerSecond = null,
        segment = segment,
    )

    @Test
    fun `la distance cumule les points successifs`() {
        val acc = TrackAccumulator.of(listOf(point(0, 48.000), point(1, 48.001), point(2, 48.002)))
        assertThat(acc.distanceMeters).isWithin(0.5).of(222.4)
        assertThat(acc.pointCount).isEqualTo(3)
    }

    @Test
    fun `aucune distance n'est comptée entre deux segments`() {
        val acc = TrackAccumulator.of(
            listOf(point(0, 48.000), point(1, 48.001), point(2, 48.010, segment = 1), point(3, 48.011, segment = 1)),
        )
        // Deux fois 111 m ; le saut de 1 km pendant la pause est ignoré.
        assertThat(acc.distanceMeters).isWithin(0.5).of(222.4)
    }

    @Test
    fun `un point trop imprécis est écarté`() {
        val acc = TrackAccumulator()
        acc.add(point(0, 48.000))
        assertThat(acc.add(point(1, 48.005, accuracy = 80f))).isFalse()
        acc.add(point(2, 48.001))
        assertThat(acc.pointCount).isEqualTo(2)
        assertThat(acc.distanceMeters).isWithin(0.5).of(111.2)
    }

    @Test
    fun `un saut à vitesse impossible n'ajoute aucune distance`() {
        // 111 m en 1 min, puis saut de 1 000 km en 1 min, puis 111 m en 1 min.
        val acc = TrackAccumulator.of(listOf(point(0, 48.000), point(1, 48.001), point(2, 57.0), point(3, 57.001)))
        assertThat(acc.distanceMeters).isWithin(0.5).of(222.4)
    }

    @Test
    fun `une première position aberrante ne bloque pas la suite de la trace`() {
        val acc = TrackAccumulator.of(listOf(point(0, 37.42), point(1, 48.000), point(2, 48.001), point(3, 48.002)))
        assertThat(acc.distanceMeters).isWithin(0.5).of(222.4)
    }

    @Test
    fun `la vitesse se déduit des deux derniers points`() {
        // 0,0018° ≈ 200,2 m en 60 s → 3,34 m/s.
        val acc = TrackAccumulator.of(listOf(point(0, 48.000), point(1, 48.0018)))
        assertThat(acc.lastSpeedMetersPerSecond!!.toDouble()).isWithin(0.05).of(3.34)
    }

    @Test
    fun `un point sans précision annoncée est conservé`() {
        assertThat(TrackAccumulator().add(point(0, 48.0, accuracy = null))).isTrue()
    }
}
