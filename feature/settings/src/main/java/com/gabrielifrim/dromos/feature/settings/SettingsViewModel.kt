package com.gabrielifrim.dromos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.model.MapPreferences
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.model.TrackingPreferences
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsAction {
    data class SelectPalette(val paletteId: String) : SettingsAction
    data class SelectThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetAutoPause(val enabled: Boolean) : SettingsAction
    data class SetOnlineBasemap(val enabled: Boolean) : SettingsAction
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val appearance: StateFlow<AppearanceSettings> = settings.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceSettings())

    val tracking: StateFlow<TrackingPreferences> = settings.tracking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingPreferences())

    val map: StateFlow<MapPreferences> = settings.map
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapPreferences())

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                is SettingsAction.SelectPalette -> settings.setPalette(action.paletteId)
                is SettingsAction.SelectThemeMode -> settings.setThemeMode(action.mode)
                is SettingsAction.SetAutoPause -> settings.setAutoPause(action.enabled)
                is SettingsAction.SetOnlineBasemap -> settings.setOnlineBasemap(action.enabled)
            }
        }
    }
}
