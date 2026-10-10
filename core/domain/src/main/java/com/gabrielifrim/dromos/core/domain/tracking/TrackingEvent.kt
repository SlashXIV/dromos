package com.gabrielifrim.dromos.core.domain.tracking

import java.time.Duration

/**
 * Moments ponctuels d'une sortie, pour les déclencheurs (bandeau, vibration,
 * notification). Contrairement à [TrackingState], un événement manqué n'est pas rejoué.
 */
sealed interface TrackingEvent {

    /** Premier point GPS reçu après le départ, une reprise ou une panne de la source. */
    data object GpsFix : TrackingEvent

    /**
     * Kilomètre [number] franchi.
     *
     * @property split temps en mouvement de ce kilomètre ; null s'il est inconnu
     *   (premier kilomètre après la récupération d'une sortie interrompue).
     * @property versusAverage écart du kilomètre à l'allure moyenne de la sortie :
     *   négatif = plus rapide que la moyenne.
     */
    data class Kilometer(val number: Int, val split: Duration?, val versusAverage: Duration?) : TrackingEvent
}

/**
 * Détecte le passage des kilomètres. [passed] et [lastAt] permettent de repartir
 * d'une sortie récupérée sans réannoncer les kilomètres déjà faits.
 */
internal class KilometerCounter(
    private var passed: Int = 0,
    private var lastAt: Duration? = Duration.ZERO,
) {
    fun onProgress(distanceMeters: Double, moving: Duration): TrackingEvent.Kilometer? {
        val km = (distanceMeters / METERS_PER_KM).toInt()
        if (km <= passed) return null
        // Plusieurs kilomètres d'un coup (GPS revenu de loin) : temps réparti également.
        val split = lastAt?.let { (moving - it).dividedBy((km - passed).toLong()) }
        passed = km
        lastAt = moving
        val average = moving.multipliedBy(METERS_PER_KM.toLong()).dividedBy(distanceMeters.toLong().coerceAtLeast(1))
        return TrackingEvent.Kilometer(km, split, split?.minus(average))
    }

    companion object {
        private const val METERS_PER_KM = 1000

        /** Compteur d'une sortie récupérée : le temps du kilomètre en cours est inconnu. */
        fun resumedAt(distanceMeters: Double) = KilometerCounter((distanceMeters / METERS_PER_KM).toInt(), lastAt = null)
    }
}
