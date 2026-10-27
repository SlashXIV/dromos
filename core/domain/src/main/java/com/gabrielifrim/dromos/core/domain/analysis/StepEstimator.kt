package com.gabrielifrim.dromos.core.domain.analysis

/**
 * Pas estimés depuis la distance et la vitesse, faute de podomètre (import GPX,
 * capteur absent). La longueur du pas croît avec la vitesse :
 * L(v) = 0,36 + 0,26·v mètres, soit ≈ 100 pas/min en marchant, ≈ 160 en courant.
 * Aveugle là où le GPS l'est : en intérieur, l'estimation sous-compte.
 */
object StepEstimator {

    fun stepLengthMeters(speedMetersPerSecond: Double): Double =
        BASE_M + SLOPE_S * speedMetersPerSecond.coerceIn(MIN_SPEED, MAX_SPEED)

    /** Pas pour [distanceMeters] parcourus en [seconds] ; sans durée, allure de marche. */
    fun steps(distanceMeters: Double, seconds: Double): Double {
        val speed = if (seconds > 0) distanceMeters / seconds else WALKING_SPEED
        return distanceMeters / stepLengthMeters(speed)
    }

    private const val BASE_M = 0.36
    private const val SLOPE_S = 0.26
    private const val MIN_SPEED = 0.5
    private const val MAX_SPEED = 6.0
    private const val WALKING_SPEED = 1.3
}
