package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class TrackAccumulatorTest {

    // 1 m vers le nord ≈ 1 / 111 195 degré de latitude.
    private val degPerMeter = 1 / 111_195.0

    private fun point(
        second: Int,
        northMeters: Double,
        segment: Int = 0,
        accuracy: Float? = 5f,
        alt: Double? = null,
        baro: Double? = null,
    ) = TrackPoint(
        timestamp = Instant.ofEpochSecond(1_760_000_000L + second),
        latitude = 48.0 + northMeters * degPerMeter, longitude = 2.0,
        altitudeMeters = alt, accuracyMeters = accuracy, speedMetersPerSecond = null,
        segment = segment, baroAltitudeMeters = baro,
    )

    /** Ligne droite vers le nord à [speed] m/s, une position par seconde, sans bruit. */
    private fun straight(seconds: Int, speed: Double, from: Int = 0, offset: Double = 0.0, segment: Int = 0) =
        (0 until seconds).map { point(from + it, offset + it * speed, segment) }

    @Test
    fun `une ligne droite sans bruit est mesurée juste`() {
        val acc = TrackAccumulator.of(straight(300, 3.0))
        // 299 pas de 3 m ; le filtre ne perd que son retard final de quelques mètres.
        assertThat(acc.distanceMeters).isWithin(897 * 0.01).of(897.0)
    }

    @Test
    fun `aucune distance n'est comptée entre deux segments`() {
        val points = straight(100, 3.0) + straight(100, 3.0, from = 400, offset = 2_000.0, segment = 1)
        val acc = TrackAccumulator.of(points)
        // Deux fois ≈ 297 m ; le saut de 1,7 km pendant la pause est ignoré.
        assertThat(acc.distanceMeters).isWithin(15.0).of(594.0)
    }

    @Test
    fun `le tremblement à l'arrêt n'ajoute presque rien`() {
        val rnd = Random(1)
        val still = (0 until 300).map { point(it, rnd.nextDouble(-4.0, 4.0)) }
        assertThat(TrackAccumulator.of(still).distanceMeters).isLessThan(15.0)
    }

    @Test
    fun `un saut à vitesse impossible n'ajoute aucune distance`() {
        val points = straight(60, 3.0) + straight(60, 3.0, from = 60, offset = 1_000_000.0)
        assertThat(TrackAccumulator.of(points).distanceMeters).isWithin(15.0).of(354.0)
    }

    @Test
    fun `une première position aberrante ne bloque pas la suite de la trace`() {
        val points = listOf(point(0, -5_000_000.0)) + straight(60, 3.0, from = 1)
        assertThat(TrackAccumulator.of(points).distanceMeters).isWithin(10.0).of(177.0)
    }

    @Test
    fun `un point trop imprécis est écarté`() {
        val acc = TrackAccumulator()
        acc.add(point(0, 0.0))
        assertThat(acc.add(point(1, 3.0, accuracy = 80f))).isFalse()
        assertThat(acc.pointCount).isEqualTo(1)
    }

    @Test
    fun `un point sans précision annoncée est conservé`() {
        assertThat(TrackAccumulator().add(point(0, 0.0, accuracy = null))).isTrue()
    }

    @Test
    fun `la vitesse lissée suit l'allure réelle`() {
        // Ajout point par point, comme en direct : of() clôt le segment et remet le filtre à zéro.
        val acc = TrackAccumulator().apply { straight(60, 3.0).forEach { add(it) } }
        assertThat(acc.smoothedSpeedMetersPerSecond!!).isWithin(0.15).of(3.0)
        assertThat(acc.instantSpeedMetersPerSecond).isWithin(0.3).of(3.0)
    }

    @Test
    fun `le dénivelé barométrique compte montée et descente`() {
        // 100 s de montée de 0,3 m/s puis 100 s de descente : 30 m de D+ et de D−.
        val points = (0 until 200).map { s ->
            val z = if (s < 100) s * 0.3 else (200 - s) * 0.3
            point(s, s * 3.0, baro = z)
        }
        val acc = TrackAccumulator.of(points)
        assertThat(acc.elevationGainMeters).isWithin(3.0).of(30.0)
        assertThat(acc.elevationLossMeters).isWithin(3.0).of(30.0)
    }

    @Test
    fun `le bruit d'altitude GPS sur le plat ne crée pas de dénivelé`() {
        val rnd = Random(2)
        val points = (0 until 600).map { s -> point(s, s * 3.0, alt = 120 + rnd.nextDouble(-6.0, 6.0)) }
        assertThat(TrackAccumulator.of(points).elevationGainMeters).isLessThan(5.0)
    }
}
