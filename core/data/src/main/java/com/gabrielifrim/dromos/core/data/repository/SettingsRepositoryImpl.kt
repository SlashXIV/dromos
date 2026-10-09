package com.gabrielifrim.dromos.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
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
            )
        }
        .distinctUntilChanged()

    override suspend fun setPalette(paletteId: String) {
        dataStore.edit { it[PALETTE] = paletteId }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override val tracking: Flow<TrackingPreferences> = dataStore.data
        .map { prefs -> TrackingPreferences(autoPause = prefs[AUTO_PAUSE] ?: true) }
        .distinctUntilChanged()

    override suspend fun setAutoPause(enabled: Boolean) {
        dataStore.edit { it[AUTO_PAUSE] = enabled }
    }

    private companion object {
        val PALETTE = stringPreferencesKey("palette")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_PAUSE = booleanPreferencesKey("pause_auto")
    }
}
