package com.gabrielifrim.dromos.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.model.MapPreferences
import com.gabrielifrim.dromos.core.domain.model.MotionPreference
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.model.TrackingPreferences
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val appearance: Flow<AppearanceSettings> = dataStore.data
        .map { prefs ->
            AppearanceSettings(
                paletteId = prefs[PALETTE],
                // Valeur inconnue (fichier modifié, version future) : on retombe sur le défaut.
                themeMode = prefs[THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                motion = prefs[MOTION]
                    ?.let { stored -> MotionPreference.entries.firstOrNull { it.name == stored } }
                    ?: MotionPreference.EXPRESSIVE,
            )
        }
        .distinctUntilChanged()

    override suspend fun setPalette(paletteId: String) {
        dataStore.edit { it[PALETTE] = paletteId }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setMotion(motion: MotionPreference) {
        dataStore.edit { it[MOTION] = motion.name }
    }

    override val tracking: Flow<TrackingPreferences> = dataStore.data
        .map { prefs ->
            TrackingPreferences(
                autoPause = prefs[AUTO_PAUSE] ?: true,
                batteryPromptDismissed = prefs[BATTERY_PROMPT_DISMISSED] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun setAutoPause(enabled: Boolean) {
        dataStore.edit { it[AUTO_PAUSE] = enabled }
    }

    override suspend fun setBatteryPromptDismissed(dismissed: Boolean) {
        dataStore.edit { it[BATTERY_PROMPT_DISMISSED] = dismissed }
    }

    override val map: Flow<MapPreferences> = dataStore.data
        .map { prefs -> MapPreferences(onlineBasemap = prefs[ONLINE_BASEMAP] ?: true) }
        .distinctUntilChanged()

    override suspend fun setOnlineBasemap(enabled: Boolean) {
        dataStore.edit { it[ONLINE_BASEMAP] = enabled }
    }

    private companion object {
        val PALETTE = stringPreferencesKey("palette")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MOTION = stringPreferencesKey("ambiance")
        val AUTO_PAUSE = booleanPreferencesKey("pause_auto")
        val ONLINE_BASEMAP = booleanPreferencesKey("carte_en_ligne")
        val BATTERY_PROMPT_DISMISSED = booleanPreferencesKey("batterie_ne_plus_demander")
    }
}
