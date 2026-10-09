package com.gabrielifrim.dromos.core.data.di

import android.content.Context
import androidx.room.Room
import com.gabrielifrim.dromos.core.data.dao.ActivityDao
import com.gabrielifrim.dromos.core.data.dao.TrackPointDao
import com.gabrielifrim.dromos.core.data.database.DromosDatabase
import com.gabrielifrim.dromos.core.data.database.applyDromosConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DromosDatabase =
        Room.databaseBuilder(context, DromosDatabase::class.java, DromosDatabase.NAME)
            .applyDromosConfiguration()
            .build()

    @Provides
    fun provideActivityDao(database: DromosDatabase): ActivityDao = database.activityDao()

    @Provides
    fun provideTrackPointDao(database: DromosDatabase): TrackPointDao = database.trackPointDao()
}
