package com.gabrielifrim.dromos.core.domain.repository

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.model.TrackTotals
import java.time.Instant

/** Écritures du moteur de suivi pendant une sortie. */
interface TrackingRepository {
    /** Crée une sortie IN_PROGRESS et renvoie son identifiant. */
    suspend fun createActivity(type: ActivityType, startedAt: Instant, autoPause: Boolean): Long

    /** Ajoute des points ET met à jour les totaux, dans une même transaction. */
    suspend fun appendPoints(activityId: Long, points: List<TrackPoint>, totals: TrackTotals)

    /** @param preview tracé simplifié pour les miniatures de l'historique. */
    suspend fun finishActivity(activityId: Long, endedAt: Instant, totals: TrackTotals, preview: List<GeoPoint>)

    suspend fun deleteActivity(activityId: Long)

    /** Sortie restée IN_PROGRESS : le processus est mort pendant l'enregistrement. */
    suspend fun findInProgress(): Activity?

    suspend fun points(activityId: Long): List<TrackPoint>
}
