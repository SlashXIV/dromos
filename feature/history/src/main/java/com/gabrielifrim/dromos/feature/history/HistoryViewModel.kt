package com.gabrielifrim.dromos.feature.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.analysis.WeekSummary
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import com.gabrielifrim.dromos.core.domain.usecase.ObserveHistoryUseCase
import com.gabrielifrim.dromos.core.gpx.GpxImporter
import com.gabrielifrim.dromos.core.gpx.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val activities: List<Activity> = emptyList(),
    /** null = toutes les sorties. */
    val filter: ActivityType? = null,
    /** Semaine en cours, toutes activités confondues (indépendant du filtre). */
    val week: WeekSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistory: ObserveHistoryUseCase,
    private val repository: ActivityRepository,
    private val importer: GpxImporter,
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ViewModel() {

    private val filter = MutableStateFlow<ActivityType?>(null)
    private val importResults = Channel<ImportResult>(Channel.BUFFERED)

    /** Bilan de chaque import, consommé une fois par l'écran. */
    val imports: Flow<ImportResult> = importResults.receiveAsFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        filter.flatMapLatest { type -> observeHistory(type) },
        observeHistory(null),
        filter,
    ) { filtered, all, type ->
        HistoryUiState(
            isLoading = false,
            activities = filtered,
            filter = type,
            week = WeekSummary.of(all, clock.instant(), ZoneId.systemDefault()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        // Sorties enregistrées avant la v0.7 : leur miniature se calcule une fois.
        viewModelScope.launch { repository.backfillPreviews() }
    }

    fun setFilter(type: ActivityType?) {
        filter.value = type
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                uris.fold(ImportResult()) { total, uri ->
                    // Document révoqué ou supprimé entre le choix et la lecture.
                    val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
                    total + (stream?.use { importer.import(it) } ?: ImportResult(rejected = 1))
                }
            }
            importResults.send(result)
        }
    }
}
