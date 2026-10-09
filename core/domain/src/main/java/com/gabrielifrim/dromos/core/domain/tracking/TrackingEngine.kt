package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.ApplicationScope
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.LocationSample
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cœur du suivi : une machine à états qui reçoit les positions, cumule les
 * mesures et écrit les points en base au fil de l'eau.
 *
 * Il ne dépend d'aucun composant Android : le service de premier plan ne fait
 * que tenir le processus en vie pendant que ce moteur travaille.
 *
 * Un point reçu est en base au plus [FLUSH_EVERY_POINTS] points plus tard, et
 * immédiatement à chaque pause ou fin : une mort du processus coûte quelques
 * secondes de trace, jamais la sortie.
 */
@Singleton
class TrackingEngine @Inject constructor(
    private val locationSource: LocationSource,
    private val repository: TrackingRepository,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var accumulator = TrackAccumulator()
    private val pending = mutableListOf<TrackPoint>()
    private var locationJob: Job? = null

    suspend fun start(type: ActivityType) = mutex.withLock {
        if (_state.value != TrackingState.Idle) return@withLock
        val now = clock.instant()
        val id = repository.createActivity(type, now)
        accumulator = TrackAccumulator()
        pending.clear()
        _state.value = TrackingState.Active(
            activityId = id, type = type, startedAt = now, paused = false, segment = 0,
            distanceMeters = 0.0, movingBase = Duration.ZERO, runningSince = now,
            pointCount = 0, lastFixAt = null, currentSpeedMetersPerSecond = null, accuracyMeters = null,
        )
        startLocationUpdates()
    }

    suspend fun pause() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        if (active.paused) return@withLock
        stopLocationUpdates()
        val now = clock.instant()
        val paused = active.copy(
            paused = true,
            movingBase = active.movingDurationAt(now),
            runningSince = null,
            currentSpeedMetersPerSecond = null,
        )
        _state.value = paused
        flush(paused)
    }

    suspend fun resume() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        if (!active.paused) return@withLock
        _state.value = active.copy(
            paused = false,
            segment = active.segment + 1,
            runningSince = clock.instant(),
            recovered = false,
            locationError = false,
        )
        startLocationUpdates()
    }

    /**
     * Termine la sortie. Une sortie sans aucun point est supprimée plutôt
     * qu'enregistrée vide.
     *
     * @return l'identifiant de la sortie enregistrée, ou null si elle a été écartée.
     */
    suspend fun stop(): Long? = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock null
        stopLocationUpdates()
        val now = clock.instant()
        val final = active.copy(movingBase = active.movingDurationAt(now), runningSince = null)
        flush(final)
        _state.value = TrackingState.Idle
        if (accumulator.pointCount == 0) {
            repository.deleteActivity(active.activityId)
            null
        } else {
            repository.finishActivity(active.activityId, now, final.distanceMeters, final.movingBase)
            active.activityId
        }
    }

    /** Abandonne la sortie en cours sans l'enregistrer. */
    suspend fun discard() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        stopLocationUpdates()
        pending.clear()
        _state.value = TrackingState.Idle
        repository.deleteActivity(active.activityId)
    }

    /**
     * Récupère une sortie interrompue par la mort du processus. Elle revient EN
     * PAUSE : reprendre l'enregistrement sans que l'utilisateur le sache
     * produirait une trace fausse (il a pu marcher sans suivi entre-temps).
     *
     * @return true si une sortie a été récupérée.
     */
    suspend fun recover(): Boolean = mutex.withLock {
        if (_state.value != TrackingState.Idle) return@withLock false
        val activity = repository.findInProgress() ?: return@withLock false
        val points = repository.points(activity.id)
        accumulator = TrackAccumulator.of(points)
        pending.clear()
        _state.value = TrackingState.Active(
            activityId = activity.id,
            type = activity.type,
            startedAt = activity.startedAt,
            paused = true,
            segment = points.maxOfOrNull { it.segment } ?: 0,
            distanceMeters = accumulator.distanceMeters,
            movingBase = activity.movingDuration,
            runningSince = null,
            pointCount = accumulator.pointCount,
            lastFixAt = points.lastOrNull()?.timestamp,
            currentSpeedMetersPerSecond = null,
            accuracyMeters = null,
            recovered = true,
        )
        true
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationSource.locations()
                .catch { markLocationError() }
                .collect { onSample(it) }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    private suspend fun markLocationError() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        _state.value = active.copy(locationError = true)
    }

    private suspend fun onSample(sample: LocationSample) = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        if (active.paused) return@withLock
        // Position datée d'avant le segment : dernière position connue en cache,
        // livrée à l'abonnement. Elle ne décrit pas cette sortie.
        val segmentStart = active.runningSince ?: return@withLock
        if (sample.timestamp < segmentStart.minusSeconds(STALE_TOLERANCE_S)) return@withLock
        val point = TrackPoint(
            timestamp = sample.timestamp,
            latitude = sample.latitude,
            longitude = sample.longitude,
            altitudeMeters = sample.altitudeMeters,
            accuracyMeters = sample.accuracyMeters,
            speedMetersPerSecond = sample.speedMetersPerSecond,
            segment = active.segment,
        )
        val kept = accumulator.add(point)
        if (kept) pending += point
        val updated = active.copy(
            distanceMeters = accumulator.distanceMeters,
            pointCount = accumulator.pointCount,
            lastFixAt = sample.timestamp,
            // Vitesse du récepteur si fournie (effet Doppler, plus fiable), sinon déduite des
            // points. Certains récepteurs annoncent 0 au lieu de « inconnue » : traité pareil.
            currentSpeedMetersPerSecond = sample.speedMetersPerSecond?.takeIf { it > 0f }
                ?: accumulator.lastSpeedMetersPerSecond,
            accuracyMeters = sample.accuracyMeters,
            locationError = false,
        )
        _state.value = updated
        if (pending.size >= FLUSH_EVERY_POINTS) flush(updated)
    }

    /** À appeler sous [mutex]. */
    private suspend fun flush(state: TrackingState.Active) {
        val batch = pending.toList()
        pending.clear()
        repository.appendPoints(
            activityId = state.activityId,
            points = batch,
            distanceMeters = state.distanceMeters,
            movingDuration = state.movingDurationAt(clock.instant()),
        )
    }

    companion object {
        /** ≈ 5 s de trace à une position par seconde. */
        const val FLUSH_EVERY_POINTS = 5

        /** Tolérance d'horloge entre le téléphone et l'horodatage GNSS. */
        const val STALE_TOLERANCE_S = 5L
    }
}
