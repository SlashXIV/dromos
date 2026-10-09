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

    private const val MIN_METERS_FOR_PACE = 10.0
}
