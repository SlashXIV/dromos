package com.gabrielifrim.dromos.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.tracking.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class RecordUiState(
    val selectedType: ActivityType = ActivityType.RUN,
    val tracking: TrackingState = TrackingState.Idle,
    val now: Instant = Instant.EPOCH,
    val trace: List<List<GeoPoint>> = emptyList(),
    val onlineBasemap: Boolean = true,
    val batteryPromptDismissed: Boolean = false,
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
}

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val controller: TrackingController,
    private val clock: Clock,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val selectedType = MutableStateFlow(ActivityType.RUN)

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
        combine(settings.map, settings.tracking) { map, prefs -> map to prefs },
    ) { type, tracking, now, trace, (map, prefs) ->
        RecordUiState(
            selectedType = type,
            tracking = tracking,
            now = now,
            trace = trace,
            onlineBasemap = map.onlineBasemap,
            batteryPromptDismissed = prefs.batteryPromptDismissed,
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
            }
        }
    }

    private companion object {
        const val TICK_MS = 500L
    }
}
