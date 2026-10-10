package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ProfileBuilderTest {

    private val degPerMeter = 1 / 111_195.0

    /** Trajet vers le nord à 1 Hz ; [altitude] donne l'altitude baromètre de chaque seconde. */
    private fun track(speeds: List<Double>, altitude: (Int) -> Double? = { null }): List<TrackPoint> {
        var north = 0.0
        return speeds.mapIndexed { i, v ->
            north += v
            TrackPoint(
                timestamp = Instant.ofEpochSecond(1_760_000_000L + i),
                latitude = 48.0 + north * degPerMeter, longitude = 2.0,
                altitudeMeters = null, accuracyMeters = 5f, speedMetersPerSecond = null, segment = 0,
                baroAltitudeMeters = altitude(i),
            )
        }
    }

    @Test
    fun `l'allure suit la vitesse et disparaît à l'arrêt`() {
        val profile = ProfileBuilder.build(track(List(120) { 3.0 } + List(60) { 0.0 }))
        val running = profile.first { it.distanceMeters > 200 }
        // 3 m/s → 333 s/km, à quelques secondes près selon le lissage.
        assertThat(running.paceSecondsPerKm!!).isWithin(15.0).of(333.3)
        assertThat(profile.last().paceSecondsPerKm).isNull()
    }

    @Test
    fun `l'altitude du baromètre est lissée, pas inventée`() {
        val profile = ProfileBuilder.build(track(List(200) { 3.0 }) { i -> 100.0 + i * 0.1 })
        assertThat(profile.first().elevationMeters!!).isWithin(1.0).of(100.0)
        assertThat(profile.last().elevationMeters!!).isWithin(1.0).of(119.7)
    }

    @Test
    fun `sans altitude, le profil d'altitude est vide`() {
        assertThat(ProfileBuilder.build(track(List(60) { 3.0 })).mapNotNull { it.elevationMeters }).isEmpty()
    }

    @Test
    fun `une longue sortie est réduite sans perdre ses bornes`() {
        val points = track(List(3_600) { 3.0 })
        val profile = ProfileBuilder.build(points, maxSamples = 300)
        assertThat(profile.size).isAtMost(300)
        assertThat(profile.map { it.distanceMeters }).isInStrictOrder()
        // La fin du graphique tombe sur la distance de la sortie.
        assertThat(profile.last().distanceMeters).isWithin(1.0).of(ProfileBuilder.build(points, maxSamples = 10_000).last().distanceMeters)
    }

    @Test
    fun `aucun point, aucun profil`() {
        assertThat(ProfileBuilder.build(emptyList())).isEmpty()
    }
}
