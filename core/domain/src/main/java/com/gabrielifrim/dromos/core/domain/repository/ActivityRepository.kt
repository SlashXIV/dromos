package com.gabrielifrim.dromos.core.domain.repository

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    /** Sorties terminées, la plus récente d'abord ; [type] null = toutes. */
    fun observeFinishedActivities(type: ActivityType? = null): Flow<List<Activity>>

    /** Une sortie, ou null si elle a été supprimée. */
    fun observeActivity(id: Long): Flow<Activity?>

    suspend fun points(id: Long): List<TrackPoint>

    /** Titre et notes vides sont enregistrés comme absents (titre par défaut affiché). */
    suspend fun updateDetails(id: Long, title: String?, notes: String?)

    suspend fun delete(id: Long)

    /** Calcule l'aperçu des sorties qui n'en ont pas (enregistrées avant la v0.7). */
    suspend fun backfillPreviews()
}
