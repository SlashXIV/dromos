package com.gabrielifrim.dromos.core.data.di

import com.gabrielifrim.dromos.core.data.repository.ActivityRepositoryImpl
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
}
