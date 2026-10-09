package com.gabrielifrim.dromos.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val activities: List<Activity> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistory: ObserveHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeHistory()
        .map { HistoryUiState(isLoading = false, activities = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
