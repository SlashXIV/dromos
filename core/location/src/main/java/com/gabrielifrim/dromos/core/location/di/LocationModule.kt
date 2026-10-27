package com.gabrielifrim.dromos.core.location.di

import com.gabrielifrim.dromos.core.domain.location.BarometerSource
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.location.StepSource
import com.gabrielifrim.dromos.core.location.GnssLocationSource
import com.gabrielifrim.dromos.core.location.PressureBarometerSource
import com.gabrielifrim.dromos.core.location.StepCounterSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationSource(impl: GnssLocationSource): LocationSource

    @Binds
    abstract fun bindBarometerSource(impl: PressureBarometerSource): BarometerSource

    @Binds
    abstract fun bindStepSource(impl: StepCounterSource): StepSource
}
