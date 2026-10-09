package com.gabrielifrim.dromos.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabrielifrim.dromos.core.data.database.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE status = :status ORDER BY started_at DESC")
    fun observeByStatus(status: String): Flow<List<ActivityEntity>>

    @Insert
    suspend fun insert(activity: ActivityEntity): Long
}
