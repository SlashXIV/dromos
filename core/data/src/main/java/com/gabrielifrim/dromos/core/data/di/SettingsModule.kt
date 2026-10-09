package com.gabrielifrim.dromos.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Une seule instance par processus : DataStore refuse deux instances sur le même fichier.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "reglages")

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
