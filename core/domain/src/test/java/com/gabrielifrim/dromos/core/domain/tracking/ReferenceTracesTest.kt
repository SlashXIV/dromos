package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.tracking.SyntheticTrack.Noise
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

/**
 * Critère d'acceptation du jalon « Métriques fiables », sur cinq parcours de
 * référence à vérité connue et plusieurs tirages de bruit.
 *
 * Traces SYNTHÉTIQUES : elles fixent l'algorithme et empêchent toute
 * régression ; la recette terrain (traces réelles) les complète.
 */
class ReferenceTracesTest {

    private val seeds = 0 until 4

    private fun distanceErrorPercent(noise: Noise, doppler: Boolean): List<Pair<String, Double>> =
        SyntheticTrack.SCENARIOS.flatMap { scenario ->
            val truth = SyntheticTrack.truth(scenario.route)
            seeds.map { seed ->
                val points = SyntheticTrack.points(scenario.route, scenario.speed, seed, noise, doppler)
                val measured = TrackAccumulator.of(points).distanceMeters
                "${scenario.name} (graine $seed)" to (measured - truth.distanceMeters) / truth.distanceMeters * 100
            }
        }

    private fun assertWithin(results: List<Pair<String, Double>>, limitPercent: Double) {
        results.forEach { (name, error) ->
            assertWithMessage("$name : erreur %.2f %%, limite ±$limitPercent %%".format(error))
                .that(abs(error)).isAtMost(limitPercent)
        }
    }

    @Test
    fun `distance à 2 pour cent avec Doppler par ciel dégagé`() =
        assertWithin(distanceErrorPercent(Noise.CLEAR_SKY, doppler = true), 2.0)

    @Test
    fun `distance à 5 pour cent avec Doppler en conditions difficiles`() =
        assertWithin(distanceErrorPercent(Noise.DIFFICULT, doppler = true), 5.0)

    @Test
    fun `distance à 5 pour cent sans Doppler par ciel dégagé`() =
        assertWithin(distanceErrorPercent(Noise.CLEAR_SKY, doppler = false), 5.0)

    @Test
    fun `dénivelé à 10 pour cent avec baromètre`() {
        SyntheticTrack.SCENARIOS.forEach { scenario ->
            val truth = SyntheticTrack.truth(scenario.route)
            seeds.forEach { seed ->
                val gain = TrackAccumulator.of(SyntheticTrack.points(scenario.route, scenario.speed, seed)).elevationGainMeters
                val name = "${scenario.name} (graine $seed) : D+ %.0f m pour %.0f m".format(gain, truth.gainMeters)
                // Parcours quasi plats : un pourcentage n'a plus de sens, on borne l'écart absolu.
                val limit = maxOf(truth.gainMeters * 0.10, 5.0)
                assertWithMessage(name).that(abs(gain - truth.gainMeters)).isAtMost(limit)
            }
        }
    }

    @Test
    fun `dénivelé GPS seul borné faute de baromètre`() {
        SyntheticTrack.SCENARIOS.forEach { scenario ->
            val truth = SyntheticTrack.truth(scenario.route)
            seeds.forEach { seed ->
                val points = SyntheticTrack.points(scenario.route, scenario.speed, seed, barometer = false)
                val gain = TrackAccumulator.of(points).elevationGainMeters
                val name = "${scenario.name} (graine $seed) : D+ GPS %.0f m pour %.0f m".format(gain, truth.gainMeters)
                // L'altitude GPS dérive de plusieurs mètres : c'est la limite physique sans
                // baromètre (hors-ligne, pas de modèle de terrain pour recaler). Borne : ±30 %,
                // ou 40 m d'écart sur un parcours peu vallonné et lent (la dérive s'accumule).
                val limit = maxOf(truth.gainMeters * 0.30, 40.0)
                assertWithMessage(name).that(abs(gain - truth.gainMeters)).isAtMost(limit)
            }
        }
    }
}
