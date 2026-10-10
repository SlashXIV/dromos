package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.analysis.ActivityAnalyzer
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import java.io.InputStream
import javax.inject.Inject

/** Bilan d'un import. */
data class ImportResult(
    val imported: Int = 0,
    /** Déjà présentes (premier point déjà en base) : un fichier réimporté. */
    val duplicates: Int = 0,
    /** Fichiers illisibles, ou traces sans horodatage. */
    val rejected: Int = 0,
) {
    operator fun plus(other: ImportResult) =
        ImportResult(imported + other.imported, duplicates + other.duplicates, rejected + other.rejected)
}

/**
 * Importe chaque trace d'un fichier GPX comme une sortie terminée. Les totaux sont
 * recalculés depuis les points avec les filtres du direct, jamais repris d'une
 * autre application : une sortie importée se compare à une sortie enregistrée.
 *
 * Lecture bloquante : à appeler hors du fil principal.
 */
class GpxImporter @Inject constructor(private val repository: ActivityRepository) {

    suspend fun import(input: InputStream): ImportResult {
        val tracks = try {
            GpxReader.read(input)
        } catch (e: Exception) {
            // SAXException, IOException : le fichier entier est illisible.
            return ImportResult(rejected = 1)
        }
        if (tracks.isEmpty()) return ImportResult(rejected = 1)
        return tracks.fold(ImportResult()) { result, track -> result + importTrack(track) }
    }

    private suspend fun importTrack(track: GpxTrack): ImportResult {
        val points = track.points
        if (points.size < 2) return ImportResult(rejected = 1)
        val type = track.type ?: guessType(track)
        val totals = ActivityAnalyzer.totals(points, type, autoPause = true)
        val activity = Activity(
            id = 0,
            type = type,
            status = ActivityStatus.FINISHED,
            startedAt = points.first().timestamp,
            endedAt = points.last().timestamp,
            distanceMeters = totals.distanceMeters,
            movingDuration = totals.movingDuration,
            elevationGainMeters = totals.elevationGainMeters,
            elevationLossMeters = totals.elevationLossMeters,
            title = track.name,
            notes = track.description,
            autoPause = true,
        )
        return if (repository.importActivity(activity, points) != null) ImportResult(imported = 1)
        else ImportResult(duplicates = 1)
    }

    /** Type absent du fichier : déduit de la vitesse moyenne en mouvement. */
    private fun guessType(track: GpxTrack): ActivityType {
        val totals = ActivityAnalyzer.totals(track.points, ActivityType.WALK, autoPause = true)
        val seconds = totals.movingDuration.toMillis() / 1000.0
        val speed = if (seconds > 0) totals.distanceMeters / seconds else 0.0
        return if (speed >= RUN_SPEED_METERS_PER_SECOND) ActivityType.RUN else ActivityType.WALK
    }

    private companion object {
        /** 8 km/h (7:30 /km) : au-delà, presque personne ne marche. */
        const val RUN_SPEED_METERS_PER_SECOND = 2.2
    }
}
