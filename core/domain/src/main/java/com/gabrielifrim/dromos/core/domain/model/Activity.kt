package com.gabrielifrim.dromos.core.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Résumé d'une sortie. Les totaux sont mis en cache à la fin de la sortie : la
 * liste de l'historique ne relit jamais les points GPS.
 */
data class Activity(
    val id: Long,
    val type: ActivityType,
    val status: ActivityStatus,
    val startedAt: Instant,
    val endedAt: Instant?,
    val distanceMeters: Double,
    val movingDuration: Duration,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val title: String?,
    val notes: String?,
    /** Pause automatique active pendant la sortie : l'analyse retire alors les arrêts du temps. */
    val autoPause: Boolean = true,
    /** Tracé simplifié (quelques dizaines de points) pour les miniatures ; null avant calcul. */
    val preview: List<GeoPoint>? = null,
    /** Pas comptés par le podomètre ; null s'il n'a rien mesuré (import, capteur absent, refus). */
    val steps: Int? = null,
)
