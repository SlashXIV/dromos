package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Générateur de traces de test à vérité connue : un parcours (points en mètres
 * avec altitude) parcouru à vitesse constante, échantillonné à 1 Hz, puis dégradé
 * par un bruit GNSS réaliste. Graine fixe : chaque exécution produit la même trace.
 *
 * Modèle de bruit (cf. littérature GNSS grand public) :
 * - horizontal : dérive lente corrélée (AR(1)) + petit bruit blanc ;
 * - altitude GPS : dérive lente de plusieurs mètres + bruit blanc ;
 * - baromètre : bruit blanc de 25 cm + dérive météo d'au plus 1 m/h ;
 * - vitesse Doppler : bruit blanc de 0,2 m/s.
 */
object SyntheticTrack {

    data class Waypoint(val x: Double, val y: Double, val z: Double)

    data class Truth(val distanceMeters: Double, val gainMeters: Double)

    data class Noise(val driftStd: Double, val whiteStd: Double, val ar: Double) {
        companion object {
            val CLEAR_SKY = Noise(driftStd = 4.0, whiteStd = 1.0, ar = 0.995)
            val DIFFICULT = Noise(driftStd = 6.0, whiteStd = 2.5, ar = 0.98)
        }
    }

    private const val ORIGIN_LAT = 45.75
    private const val ORIGIN_LON = 4.83
    private const val M_PER_DEG = 6_371_008.8 * Math.PI / 180

    fun truth(route: List<Waypoint>): Truth {
        var d = 0.0
        var g = 0.0
        route.zipWithNext { a, b ->
            d += hypot(b.x - a.x, b.y - a.y)
            if (b.z > a.z) g += b.z - a.z
        }
        return Truth(d, g)
    }

    /** Positions vraies (t, x, y, z) à vitesse constante, une par seconde. */
    private fun sample(route: List<Waypoint>, speed: Double): List<DoubleArray> {
        val legs = route.zipWithNext()
        val lengths = legs.map { (a, b) -> hypot(b.x - a.x, b.y - a.y) }
        val total = lengths.sum()
        val out = mutableListOf<DoubleArray>()
        var s = 0.0
        var t = 0.0
        var leg = 0
        var legStart = 0.0
        while (s <= total) {
            while (leg < legs.size - 1 && s > legStart + lengths[leg]) {
                legStart += lengths[leg]; leg++
            }
            val (a, b) = legs[leg]
            val f = if (lengths[leg] > 0) ((s - legStart) / lengths[leg]).coerceIn(0.0, 1.0) else 0.0
            out += doubleArrayOf(t, a.x + f * (b.x - a.x), a.y + f * (b.y - a.y), a.z + f * (b.z - a.z))
            s += speed; t += 1.0
        }
        return out
    }

    fun points(
        route: List<Waypoint>,
        speed: Double,
        seed: Int,
        noise: Noise = Noise.CLEAR_SKY,
        doppler: Boolean = true,
        barometer: Boolean = true,
    ): List<TrackPoint> {
        val rnd = Random(seed)
        fun gauss(std: Double): Double {
            // Box-Muller : Random de Kotlin n'a pas de gaussienne.
            val u1 = rnd.nextDouble().coerceAtLeast(1e-12)
            val u2 = rnd.nextDouble()
            return std * sqrt(-2 * kotlin.math.ln(u1)) * cos(2 * Math.PI * u2)
        }
        val truthPoints = sample(route, speed)
        val inn = sqrt(1 - noise.ar * noise.ar)
        val arZ = 0.99
        val innZ = sqrt(1 - arZ * arZ)
        val baroDrift = (rnd.nextDouble() * 2 - 1) / 3600
        var bx = 0.0; var by = 0.0; var bz = 0.0
        val mPerDegLon = M_PER_DEG * cos(Math.toRadians(ORIGIN_LAT))
        return truthPoints.mapIndexed { i, p ->
            val (t, x, y, z) = p.toList()
            bx = noise.ar * bx + gauss(noise.driftStd * inn)
            by = noise.ar * by + gauss(noise.driftStd * inn)
            bz = arZ * bz + gauss(6.0 * innZ)
            val nx = x + bx + gauss(noise.whiteStd)
            val ny = y + by + gauss(noise.whiteStd)
            var speedValue: Float? = null
            var bearing: Float? = null
            if (doppler) {
                val prev = truthPoints[maxOf(0, i - 1)]
                val next = truthPoints[minOf(truthPoints.size - 1, i + 1)]
                val dt = (next[0] - prev[0]).takeIf { it > 0 } ?: 1.0
                val vx = (next[1] - prev[1]) / dt + gauss(0.2)
                val vy = (next[2] - prev[2]) / dt + gauss(0.2)
                speedValue = hypot(vx, vy).toFloat()
                bearing = ((Math.toDegrees(atan2(vx, vy)) + 360) % 360).toFloat()
            }
            TrackPoint(
                timestamp = Instant.ofEpochSecond(1_760_000_000L + t.toLong()),
                latitude = ORIGIN_LAT + ny / M_PER_DEG,
                longitude = ORIGIN_LON + nx / mPerDegLon,
                altitudeMeters = z + bz + gauss(3.0),
                accuracyMeters = 5f,
                speedMetersPerSecond = speedValue,
                segment = 0,
                bearingDegrees = bearing,
                baroAltitudeMeters = if (barometer) z + baroDrift * t + gauss(0.25) else null,
            )
        }
    }

    // ---------------------------------------------------------------------
    //  Les cinq parcours de référence
    // ---------------------------------------------------------------------

    data class Scenario(val name: String, val route: List<Waypoint>, val speed: Double)

    val SCENARIOS: List<Scenario> = listOf(
        Scenario(
            "boucle plate 5 km, course",
            (0..40).map { a ->
                val r = 800.0
                val angle = a / 40.0 * 2 * Math.PI
                Waypoint(r * cos(angle), r * kotlin.math.sin(angle), 0.0)
            },
            speed = 3.0,
        ),
        Scenario(
            "trail vallonné 10 km, 4 bosses",
            (0..100).map { i ->
                Waypoint(i * 100.0, 30 * kotlin.math.sin(i / 7.0), 60 * maxOf(0.0, kotlin.math.sin(i * Math.PI / 25)))
            },
            speed = 2.6,
        ),
        Scenario(
            "piste 400 m, 8 tours",
            (0 until 8).flatMap { lap ->
                (0 until 24).map { k ->
                    val angle = Math.toRadians(k * 15.0)
                    Waypoint(60 * cos(angle), 36 * kotlin.math.sin(angle), 0.0)
                }
            } + Waypoint(60.0, 0.0, 0.0),
            speed = 4.0,
        ),
        Scenario(
            "marche urbaine en zigzag 3 km",
            buildList {
                var x = 0.0; var y = 0.0
                add(Waypoint(0.0, 0.0, 0.0))
                repeat(30) { i ->
                    if (i % 2 == 0) x += 100 else y += 100
                    add(Waypoint(x, y, 8 * kotlin.math.sin(i / 3.0)))
                }
            },
            speed = 1.3,
        ),
        Scenario(
            "semi-marathon, 150 m de montée",
            (0..100).map { i -> Waypoint(i * 210.0, 0.0, 150 * kotlin.math.sin(i / 100.0 * Math.PI)) },
            speed = 3.3,
        ),
    )
}
