package com.gabrielifrim.dromos.core.ui.text

import java.time.Duration
import java.util.Locale

/**
 * Mise en forme des mesures, sans unité : l'unité vient de strings.xml, pour que
 * l'espace insécable et l'ordre restent l'affaire de chaque langue.
 */
object Format {

    /** 10 012,5 m → « 10,01 » (km, deux décimales). */
    fun kilometers(meters: Double, locale: Locale): String =
        String.format(locale, "%.2f", meters / 1000.0)

    /** 52 min 9 s → « 52:09 » ; 1 h 5 min 3 s → « 1:05:03 ». */
    fun duration(duration: Duration): String {
        val totalSeconds = duration.seconds.coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /**
     * Allure en minutes par kilomètre : « 5:12 ». Null sous 10 m parcourus, où
     * l'allure n'a pas de sens (elle tendrait vers l'infini).
     */
    fun pace(duration: Duration, meters: Double): String? {
        if (meters < MIN_METERS_FOR_PACE) return null
        val secondsPerKm = Math.round(duration.seconds * 1000.0 / meters)
        return "%d:%02d".format(secondsPerKm / 60, secondsPerKm % 60)
    }

    /**
     * Allure tirée d'une vitesse instantanée : « 5:12 ». Null sous 0,5 m/s
     * (≈ 33 min/km) : à l'arrêt, l'allure n'a plus de sens.
     */
    fun paceFromSpeed(metersPerSecond: Float): String? {
        if (metersPerSecond < MIN_SPEED_FOR_PACE) return null
        val secondsPerKm = Math.round(1000.0 / metersPerSecond)
        return "%d:%02d".format(secondsPerKm / 60, secondsPerKm % 60)
    }

    /** Dénivelé arrondi au mètre : « 84 ». */
    fun meters(value: Double): String = Math.round(value).toString()

    /** 312,4 s/km → « 5:12 ». */
    fun paceFromSeconds(secondsPerKm: Double): String {
        val s = Math.round(secondsPerKm)
        return "%d:%02d".format(s / 60, s % 60)
    }

    private const val MIN_METERS_FOR_PACE = 10.0
    private const val MIN_SPEED_FOR_PACE = 0.5f
}
