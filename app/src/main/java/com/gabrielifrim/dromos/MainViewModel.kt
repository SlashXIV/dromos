package com.gabrielifrim.dromos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {

    /** Null tant que les réglages ne sont pas lus : l'écran de démarrage reste affiché. */
    val appearance: StateFlow<AppearanceSettings?> = settings.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
