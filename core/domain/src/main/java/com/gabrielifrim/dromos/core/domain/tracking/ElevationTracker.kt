package com.gabrielifrim.dromos.core.domain.tracking

/**
 * Dénivelé positif et négatif par hystérésis : une montée n'est comptée que
 * lorsque l'altitude lissée a dépassé de [Source.hysteresis] le dernier point
 * de référence. Le bruit, qui oscille autour d'une valeur, ne franchit pas le seuil.
 *
 * Le baromètre est préféré : sa mesure relative est précise au mètre près. Le
 * GPS ne sert qu'à défaut : son altitude dérive de plusieurs mètres, d'où un
 * lissage et un seuil bien plus larges (et une précision moindre, ≈ 20 %).
 */
internal class ElevationTracker {

    enum class Source(val window: Int, val hysteresis: Double) {
        BAROMETER(window = 5, hysteresis = 2.0),
        GPS(window = 30, hysteresis = 12.0),
    }

    var gainMeters = 0.0
        private set
    var lossMeters = 0.0
        private set

    /** Altitude lissée du moment, pour le profil ; null avant la première mesure. */
    val smoothedMeters: Double? get() = if (buffer.isEmpty()) null else buffer.average()

    private var source: Source? = null
    private val buffer = ArrayDeque<Double>()
    private var reference: Double? = null

    fun add(baroAltitude: Double?, gpsAltitude: Double?) {
        val (altitude, from) = when {
            baroAltitude != null -> baroAltitude to Source.BAROMETER
            gpsAltitude != null -> gpsAltitude to Source.GPS
            else -> return
        }
        if (from != source) {
            // Deux sources n'ont pas le même zéro : repartir d'une nouvelle référence.
            source = from
            buffer.clear()
            reference = null
        }
        buffer.addLast(altitude)
        while (buffer.size > from.window) buffer.removeFirst()
        val smoothed = buffer.average()
        val ref = reference
        when {
            ref == null -> reference = smoothed
            smoothed - ref >= from.hysteresis -> { gainMeters += smoothed - ref; reference = smoothed }
            ref - smoothed >= from.hysteresis -> { lossMeters += ref - smoothed; reference = smoothed }
        }
    }

    /** Coupure (pause manuelle) : le dénivelé franchi pendant la pause n'est pas compté. */
    fun reset() {
        buffer.clear()
        reference = null
    }
}
