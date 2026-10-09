package com.gabrielifrim.dromos.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gabrielifrim.dromos.core.data.dao.ActivityDao
import com.gabrielifrim.dromos.core.data.dao.TrackPointDao
import com.gabrielifrim.dromos.core.data.database.entity.ActivityEntity
import com.gabrielifrim.dromos.core.data.database.entity.TrackPointEntity

@Database(
    entities = [ActivityEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DromosDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        const val NAME = "dromos.db"
    }
}
