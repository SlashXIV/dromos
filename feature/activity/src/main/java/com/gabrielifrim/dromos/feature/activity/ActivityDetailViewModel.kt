package com.gabrielifrim.dromos.feature.activity

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.analysis.ActivityAnalyzer
import com.gabrielifrim.dromos.core.domain.analysis.Split
import com.gabrielifrim.dromos.core.domain.analysis.ProfileSample
import com.gabrielifrim.dromos.core.domain.analysis.ProfileBuilder
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ce qui se calcule une fois, depuis les points : tracé et splits. */
data class ActivityAnalysis(
    val trace: List<List<GeoPoint>>,
    val splits: List<Split>,
    val profile: List<ProfileSample> = emptyList(),
    /** Pas estimés depuis la distance, quand le podomètre n'a rien mesuré. */
    val estimatedSteps: Int? = null,
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

    /** [name] : le titre affiché, titre par défaut compris. */
    data class Share(val format: ExportFormat, val name: String) : ActivityDetailAction
    data class Export(val format: ExportFormat, val name: String, val uri: Uri) : ActivityDetailAction
}

/** Issue d'un export, consommée une fois par l'écran. */
sealed interface ExportEvent {
    data class Share(val uri: Uri, val format: ExportFormat) : ExportEvent
    data object Saved : ExportEvent
    data object Failed : ExportEvent
}

@HiltViewModel(assistedFactory = ActivityDetailViewModel.Factory::class)
class ActivityDetailViewModel @AssistedInject constructor(
    @Assisted private val activityId: Long,
    private val repository: ActivityRepository,
    private val exporter: ActivityExporter,
    settings: SettingsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(activityId: Long): ActivityDetailViewModel
    }

    private val analysis = MutableStateFlow<ActivityAnalysis?>(null)
    private val deleted = MutableStateFlow(false)
    private var points: List<TrackPoint> = emptyList()
    private val exportEvents = Channel<ExportEvent>(Channel.BUFFERED)
    val exports: Flow<ExportEvent> = exportEvents.receiveAsFlow()

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
            val points = repository.points(activityId).also { this@ActivityDetailViewModel.points = it }
            // Les points d'une sortie terminée ne changent plus : une seule analyse,
            // hors du fil principal (une sortie de 2 h, c'est 7 200 points rejoués).
            analysis.value = withContext(Dispatchers.Default) {
                ActivityAnalysis(
                    trace = points.groupBy { it.segment }.toSortedMap().values
                        .map { segment -> segment.map { GeoPoint(it.latitude, it.longitude) } },
                    splits = ActivityAnalyzer.splits(points, activity.type, activity.autoPause),
                    profile = ProfileBuilder.build(points),
                    estimatedSteps = if (activity.steps == null) {
                        ActivityAnalyzer.estimatedSteps(points, activity.type, activity.autoPause)
                    } else {
                        null
                    },
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
                is ActivityDetailAction.Share -> export {
                    ExportEvent.Share(exporter.shareable(it, action.name, points, action.format), action.format)
                }
                is ActivityDetailAction.Export -> export {
                    exporter.save(it, action.name, points, action.format, action.uri)
                    ExportEvent.Saved
                }
            }
        }
    }

    private suspend fun export(block: suspend (Activity) -> ExportEvent) {
        val activity = uiState.value.activity ?: return
        val event = try {
            block(activity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Stockage plein, document révoqué, fournisseur de fichiers défaillant.
            ExportEvent.Failed
        }
        exportEvents.send(event)
    }
}
