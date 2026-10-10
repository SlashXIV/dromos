package com.gabrielifrim.dromos.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.tracking.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** GPS avant le départ : ce que montre le sonar. */
enum class GpsReadiness { OFF, SEARCHING, READY }

data class GpsWarmup(val readiness: GpsReadiness = GpsReadiness.OFF, val accuracyMeters: Float? = null)

data class RecordUiState(
    val selectedType: ActivityType = ActivityType.RUN,
    val tracking: TrackingState = TrackingState.Idle,
    val now: Instant = Instant.EPOCH,
    val trace: List<List<GeoPoint>> = emptyList(),
    val onlineBasemap: Boolean = true,
    val batteryPromptDismissed: Boolean = false,
    val countdown: Boolean = true,
    val gps: GpsWarmup = GpsWarmup(),
) {
    val active: TrackingState.Active? get() = tracking as? TrackingState.Active
    val movingDuration: Duration get() = active?.movingDurationAt(now) ?: Duration.ZERO
}

sealed interface RecordAction {
    data class SelectType(val type: ActivityType) : RecordAction
    data object Start : RecordAction
    data object Pause : RecordAction
    data object Resume : RecordAction
    data object Stop : RecordAction
    data object Discard : RecordAction
    data object NeverAskBattery : RecordAction

    /** Permission de localisation accordée : le GPS peut chauffer avant le départ. */
    data object LocationGranted : RecordAction
}

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val controller: TrackingController,
    private val clock: Clock,
    private val settings: SettingsRepository,
    private val locationSource: LocationSource,
) : ViewModel() {

    private val selectedType = MutableStateFlow(ActivityType.RUN)
    private val locationGranted = MutableStateFlow(false)

    /**
     * GPS allumé sur l'écran de départ seulement (permission accordée, aucune sortie en
     * cours, écran visible) : la première position est souvent là avant la fin du 3-2-1.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val warmup: Flow<GpsWarmup> = combine(locationGranted, controller.state) { granted, tracking ->
        granted && tracking == TrackingState.Idle
    }
        .distinctUntilChanged()
        .flatMapLatest { on ->
            if (!on) {
                flowOf(GpsWarmup())
            } else {
                locationSource.locations()
                    .map { sample ->
                        val accuracy = sample.accuracyMeters
                        val ready = accuracy == null || accuracy <= READY_ACCURACY_M
                        GpsWarmup(if (ready) GpsReadiness.READY else GpsReadiness.SEARCHING, accuracy)
                    }
                    .onStart { emit(GpsWarmup(GpsReadiness.SEARCHING)) }
                    .catch { emit(GpsWarmup()) }
            }
        }

    /** Horloge de l'écran : le chrono avance même sans nouvelle position. */
    private val ticker = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK_MS)
        }
    }

    val uiState: StateFlow<RecordUiState> = combine(
        selectedType,
        controller.state,
        ticker,
        controller.trace,
        combine(settings.map, settings.tracking, warmup) { map, prefs, gps -> Triple(map, prefs, gps) },
    ) { type, tracking, now, trace, (map, prefs, gps) ->
        RecordUiState(
            selectedType = type,
            tracking = tracking,
            now = now,
            trace = trace,
            onlineBasemap = map.onlineBasemap,
            batteryPromptDismissed = prefs.batteryPromptDismissed,
            countdown = prefs.countdown,
            gps = gps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordUiState())

    /** @param onFinished identifiant de la sortie enregistrée, null si elle a été écartée. */
    fun onAction(action: RecordAction, onFinished: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            when (action) {
                is RecordAction.SelectType -> selectedType.value = action.type
                RecordAction.Start -> controller.start(selectedType.value)
                RecordAction.Pause -> controller.pause()
                RecordAction.Resume -> controller.resume()
                RecordAction.Stop -> onFinished(controller.stop())
                RecordAction.Discard -> {
                    controller.discard()
                    onFinished(null)
                }
                RecordAction.NeverAskBattery -> settings.setBatteryPromptDismissed(true)
                RecordAction.LocationGranted -> locationGranted.value = true
            }
        }
    }

    private companion object {
        const val TICK_MS = 500L

        /** Précision suffisante pour partir : au-delà, les premiers mètres seraient faux. */
        const val READY_ACCURACY_M = 20f
    }
}
