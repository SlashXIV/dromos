package com.gabrielifrim.dromos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.tracking.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    private val tracking: TrackingController,
) : ViewModel() {

    /** Null tant que les réglages ne sont pas lus : l'écran de démarrage reste affiché. */
    val appearance: StateFlow<AppearanceSettings?> = settings.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val trackingState: StateFlow<TrackingState> get() = tracking.state

    private val _recoveryChecked = MutableStateFlow(false)

    /** Faux tant qu'on ne sait pas si une sortie interrompue attend en base. */
    val recoveryChecked: StateFlow<Boolean> = _recoveryChecked.asStateFlow()

    init {
        viewModelScope.launch {
            tracking.recover()
            _recoveryChecked.value = true
        }
    }
}
