package com.gabrielifrim.dromos.core.domain.repository

import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.model.MapPreferences
import com.gabrielifrim.dromos.core.domain.model.MotionPreference
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.model.TrackingPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val appearance: Flow<AppearanceSettings>

    suspend fun setPalette(paletteId: String)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setMotion(motion: MotionPreference)

    val tracking: Flow<TrackingPreferences>

    suspend fun setAutoPause(enabled: Boolean)

    suspend fun setBatteryPromptDismissed(dismissed: Boolean)

    suspend fun setCountdown(enabled: Boolean)

    val map: Flow<MapPreferences>

    suspend fun setOnlineBasemap(enabled: Boolean)
}
