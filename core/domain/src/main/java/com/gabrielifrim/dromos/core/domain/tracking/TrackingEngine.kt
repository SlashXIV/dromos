package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.ApplicationScope
import com.gabrielifrim.dromos.core.domain.geo.TraceSimplifier
import com.gabrielifrim.dromos.core.domain.location.BarometerSource
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.model.LocationSample
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.model.TrackTotals
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import com.gabrielifrim.dromos.core.domain.repository.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
    private val barometerSource: BarometerSource,
    private val repository: TrackingRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _trace = MutableStateFlow<List<List<GeoPoint>>>(emptyList())

    /**
     * Tracé de la sortie en cours, un segment par période d'enregistrement : la
     * carte ne relie pas deux segments, comme la distance ne les compte pas.
     */
    val trace: StateFlow<List<List<GeoPoint>>> = _trace.asStateFlow()

    // Sans abonné, l'événement est perdu : il ne décrit qu'un instant.
    private val _events = MutableSharedFlow<TrackingEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Kilomètres franchis et accroches GPS, au moment où ils arrivent. */
    val events: SharedFlow<TrackingEvent> = _events.asSharedFlow()

    private val mutex = Mutex()
    private var accumulator = TrackAccumulator()
    private var autoPause = AutoPauseDetector(ActivityType.RUN)
    private val pending = mutableListOf<TrackPoint>()
    private var locationJob: Job? = null
    private var barometerJob: Job? = null
    private var latestBaro: Pair<Instant, Double>? = null
    private var traceSegment: Int? = null
    private var kilometers = KilometerCounter()
    private var awaitingFix = false

    @Volatile
    private var autoPauseEnabled = true

    init {
        scope.launch {
            settings.tracking.collect { prefs ->
                autoPauseEnabled = prefs.autoPause
                // Désactivée en pleine pause automatique : le chrono repart tout de suite.
                if (!prefs.autoPause) mutex.withLock { leaveAutoPause(clock.instant()) }
            }
        }
    }

    suspend fun start(type: ActivityType) = mutex.withLock {
        if (_state.value != TrackingState.Idle) return@withLock
        val now = clock.instant()
        val id = repository.createActivity(type, now, autoPauseEnabled)
        accumulator = TrackAccumulator()
        kilometers = KilometerCounter()
        awaitingFix = true
        autoPause = AutoPauseDetector(type)
        pending.clear()
        _trace.value = emptyList()
        traceSegment = null
        _state.value = TrackingState.Active(
            activityId = id, type = type, startedAt = now, paused = false, segment = 0,
            distanceMeters = 0.0, movingBase = Duration.ZERO, runningSince = now,
            pointCount = 0, lastFixAt = null, currentSpeedMetersPerSecond = null, accuracyMeters = null,
        )
        startSensors()
    }

    suspend fun pause() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        if (active.paused) return@withLock
        stopSensors()
        accumulator.closeSegment()
        val now = clock.instant()
        val paused = active.copy(
            distanceMeters = accumulator.distanceMeters,
            paused = true,
            autoPaused = false,
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
        autoPause.reset()
        _state.value = active.copy(
            paused = false,
            segment = active.segment + 1,
            runningSince = clock.instant(),
            recovered = false,
            locationError = false,
        )
        awaitingFix = true
        startSensors()
    }

    /**
     * Termine la sortie. Une sortie sans aucun point est supprimée plutôt
     * qu'enregistrée vide.
     *
     * @return l'identifiant de la sortie enregistrée, ou null si elle a été écartée.
     */
    suspend fun stop(): Long? = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock null
        stopSensors()
        accumulator.closeSegment()
        val now = clock.instant()
        val final = active.copy(
            distanceMeters = accumulator.distanceMeters,
            movingBase = active.movingDurationAt(now),
            runningSince = null,
        )
        flush(final)
        val preview = TraceSimplifier.simplify(_trace.value.flatten())
        _state.value = TrackingState.Idle
        _trace.value = emptyList()
        traceSegment = null
        if (accumulator.pointCount == 0) {
            repository.deleteActivity(active.activityId)
            null
        } else {
            repository.finishActivity(active.activityId, now, final.totals(now), preview)
            active.activityId
        }
    }

    /** Abandonne la sortie en cours sans l'enregistrer. */
    suspend fun discard() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        stopSensors()
        pending.clear()
        _state.value = TrackingState.Idle
        _trace.value = emptyList()
        traceSegment = null
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
        kilometers = KilometerCounter.resumedAt(accumulator.distanceMeters)
        autoPause = AutoPauseDetector(activity.type)
        pending.clear()
        _trace.value = points.groupBy { it.segment }.toSortedMap().values
            .map { segment -> segment.map { GeoPoint(it.latitude, it.longitude) } }
        traceSegment = points.lastOrNull()?.segment
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
            elevationGainMeters = accumulator.elevationGainMeters,
            elevationLossMeters = accumulator.elevationLossMeters,
            recovered = true,
        )
        true
    }

    private fun startSensors() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationSource.locations()
                .catch { markLocationError() }
                .collect { onSample(it) }
        }
        barometerJob?.cancel()
        latestBaro = null
        barometerJob = scope.launch {
            // Pas de baromètre, ou capteur en échec : le dénivelé se rabat sur le GPS.
            barometerSource.altitudes()
                .catch { }
                .collect { latestBaro = clock.instant() to it }
        }
    }

    private fun stopSensors() {
        locationJob?.cancel()
        locationJob = null
        barometerJob?.cancel()
        barometerJob = null
    }

    private suspend fun markLocationError() = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        _state.value = active.copy(locationError = true)
        awaitingFix = true
    }

    private suspend fun onSample(sample: LocationSample) = mutex.withLock {
        val active = _state.value as? TrackingState.Active ?: return@withLock
        if (active.paused) return@withLock
        // Position datée d'avant la reprise : dernière position connue en cache,
        // livrée à l'abonnement. Elle ne décrit pas cette sortie.
        val now = clock.instant()
        if (sample.timestamp < lastResumeOrStart(active).minusSeconds(STALE_TOLERANCE_S)) return@withLock
        if (awaitingFix) {
            awaitingFix = false
            _events.tryEmit(TrackingEvent.GpsFix)
        }
        val baro = latestBaro?.takeIf { Duration.between(it.first, now) <= BARO_MAX_AGE }?.second
        val point = TrackPoint(
            timestamp = sample.timestamp,
            latitude = sample.latitude,
            longitude = sample.longitude,
            altitudeMeters = sample.altitudeMeters,
            accuracyMeters = sample.accuracyMeters,
            speedMetersPerSecond = sample.speedMetersPerSecond,
            segment = active.segment,
            bearingDegrees = sample.bearingDegrees,
            baroAltitudeMeters = baro,
        )
        val kept = accumulator.add(point)
        if (kept) {
            pending += point
            appendToTrace(point)
        }
        var updated = active.copy(
            distanceMeters = accumulator.distanceMeters,
            pointCount = accumulator.pointCount,
            lastFixAt = sample.timestamp,
            currentSpeedMetersPerSecond = accumulator.smoothedSpeedMetersPerSecond?.toFloat(),
            accuracyMeters = sample.accuracyMeters,
            elevationGainMeters = accumulator.elevationGainMeters,
            elevationLossMeters = accumulator.elevationLossMeters,
            locationError = false,
        )
        if (autoPauseEnabled && kept) {
            when (val decision = autoPause.onSpeed(now, accumulator.instantSpeedMetersPerSecond, updated.autoPaused)) {
                is AutoPauseDetector.Decision.Stop -> updated = updated.copy(
                    autoPaused = true,
                    movingBase = updated.movingDurationAt(maxOf(decision.since, updated.runningSince ?: decision.since)),
                    runningSince = null,
                )
                is AutoPauseDetector.Decision.Go -> updated = updated.copy(autoPaused = false, runningSince = decision.since)
                AutoPauseDetector.Decision.None -> Unit
            }
        }
        _state.value = updated
        kilometers.onProgress(updated.distanceMeters, updated.movingDurationAt(now))?.let { _events.tryEmit(it) }
        if (pending.size >= FLUSH_EVERY_POINTS) flush(updated)
    }

    /** Nouveau segment au premier point qui suit une reprise ; sinon, le point prolonge le dernier. */
    private fun appendToTrace(point: TrackPoint) {
        val geo = GeoPoint(point.latitude, point.longitude)
        val current = _trace.value
        val lastSegment = traceSegment
        _trace.value = if (current.isEmpty() || lastSegment != point.segment) {
            current + listOf(listOf(geo))
        } else {
            current.dropLast(1) + listOf(current.last() + geo)
        }
        traceSegment = point.segment
    }

    /** À appeler sous [mutex]. */
    private fun leaveAutoPause(now: Instant) {
        val active = _state.value as? TrackingState.Active ?: return
        if (!active.autoPaused) return
        autoPause.reset()
        _state.value = active.copy(autoPaused = false, runningSince = now)
    }

    private fun lastResumeOrStart(active: TrackingState.Active): Instant =
        active.runningSince ?: active.lastFixAt ?: active.startedAt

    /** À appeler sous [mutex]. */
    private suspend fun flush(state: TrackingState.Active) {
        val batch = pending.toList()
        pending.clear()
        repository.appendPoints(state.activityId, batch, state.totals(clock.instant()))
    }

    private fun TrackingState.Active.totals(now: Instant) = TrackTotals(
        distanceMeters = distanceMeters,
        movingDuration = movingDurationAt(now),
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = elevationLossMeters,
    )

    companion object {
        /** ≈ 5 s de trace à une position par seconde. */
        const val FLUSH_EVERY_POINTS = 5

        /** Tolérance d'horloge entre le téléphone et l'horodatage GNSS. */
        const val STALE_TOLERANCE_S = 5L

        /** Au-delà, la dernière pression connue ne décrit plus le point. */
        private val BARO_MAX_AGE: Duration = Duration.ofSeconds(5)
    }
}
