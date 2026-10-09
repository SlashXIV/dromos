package com.gabrielifrim.dromos.core.data.di

import com.gabrielifrim.dromos.core.data.repository.ActivityRepositoryImpl
import com.gabrielifrim.dromos.core.data.repository.SettingsRepositoryImpl
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
