package com.gabrielifrim.dromos.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val activities: List<Activity> = emptyList(),
    /** null = toutes les sorties. */
    val filter: ActivityType? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistory: ObserveHistoryUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow<ActivityType?>(null)

    val uiState: StateFlow<HistoryUiState> = filter
        .flatMapLatest { type -> observeHistory(type).map { HistoryUiState(isLoading = false, activities = it, filter = type) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(type: ActivityType?) {
        filter.value = type
    }
}
