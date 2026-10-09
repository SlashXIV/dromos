package com.gabrielifrim.dromos.core.tracking.di

import com.gabrielifrim.dromos.core.domain.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TrackingModule {

    /** SupervisorJob : l'échec d'une tâche (une écriture) n'annule pas le suivi entier. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
