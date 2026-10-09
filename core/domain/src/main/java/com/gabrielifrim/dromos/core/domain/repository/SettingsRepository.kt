package com.gabrielifrim.dromos.core.domain.repository

import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val appearance: Flow<AppearanceSettings>

    suspend fun setPalette(paletteId: String)

    suspend fun setThemeMode(mode: ThemeMode)
}
