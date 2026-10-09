package com.gabrielifrim.dromos.core.domain.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    /** Rayon terrestre moyen (IUGG). L'erreur de la sphère reste sous 0,5 % : bien en deçà du bruit GPS. */
    const val EARTH_RADIUS_METERS = 6_371_008.8

    /** Distance orthodromique (formule de haversine), en mètres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = phi2 - phi1
        val dLambda = Math.toRadians(lon2 - lon1)
        val h = sin(dPhi / 2).let { it * it } + cos(phi1) * cos(phi2) * sin(dLambda / 2).let { it * it }
        // min(1, …) : l'arrondi flottant peut dépasser 1 pour deux points antipodaux.
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceAtMost(1.0)))
    }
}
