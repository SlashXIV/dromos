package com.gabrielifrim.dromos.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.analysis.ActivityAnalyzer
import com.gabrielifrim.dromos.core.domain.analysis.Split
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ce qui se calcule une fois, depuis les points : tracé et splits. */
data class ActivityAnalysis(
    val trace: List<List<GeoPoint>>,
    val splits: List<Split>,
)

data class ActivityDetailUiState(
    val activity: Activity? = null,
    val analysis: ActivityAnalysis? = null,
    val onlineBasemap: Boolean = true,
    /** La sortie a été supprimée : l'écran se ferme. */
    val deleted: Boolean = false,
)

sealed interface ActivityDetailAction {
    data class Save(val title: String, val notes: String) : ActivityDetailAction
    data object Delete : ActivityDetailAction
}

@HiltViewModel(assistedFactory = ActivityDetailViewModel.Factory::class)
class ActivityDetailViewModel @AssistedInject constructor(
    @Assisted private val activityId: Long,
    private val repository: ActivityRepository,
    settings: SettingsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(activityId: Long): ActivityDetailViewModel
    }

    private val analysis = MutableStateFlow<ActivityAnalysis?>(null)
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<ActivityDetailUiState> = combine(
        repository.observeActivity(activityId),
        analysis,
        settings.map,
        deleted,
    ) { activity, analysis, map, deleted ->
        ActivityDetailUiState(activity, analysis, map.onlineBasemap, deleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityDetailUiState())

    init {
        viewModelScope.launch {
            val activity = repository.observeActivity(activityId).filterNotNull().first()
            val points = repository.points(activityId)
            // Les points d'une sortie terminée ne changent plus : une seule analyse,
            // hors du fil principal (une sortie de 2 h, c'est 7 200 points rejoués).
            analysis.value = withContext(Dispatchers.Default) {
                ActivityAnalysis(
                    trace = points.groupBy { it.segment }.toSortedMap().values
                        .map { segment -> segment.map { GeoPoint(it.latitude, it.longitude) } },
                    splits = ActivityAnalyzer.splits(points, activity.type, activity.autoPause),
                )
            }
        }
    }

    fun onAction(action: ActivityDetailAction) {
        viewModelScope.launch {
            when (action) {
                is ActivityDetailAction.Save -> repository.updateDetails(activityId, action.title, action.notes)
                ActivityDetailAction.Delete -> {
                    repository.delete(activityId)
                    deleted.value = true
                }
            }
        }
    }
}
