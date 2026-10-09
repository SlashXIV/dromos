package com.gabrielifrim.dromos.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.analysis.WeekSummary
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import com.gabrielifrim.dromos.core.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val clock: Clock,
) : ViewModel() {

    private val filter = MutableStateFlow<ActivityType?>(null)

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
}
