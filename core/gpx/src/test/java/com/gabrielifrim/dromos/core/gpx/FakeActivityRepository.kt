package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

/** Ne connaît que l'import, avec la même règle de doublon que la base. */
class FakeActivityRepository : ActivityRepository {
    val saved = mutableListOf<Activity>()
    private val timestamps = mutableSetOf<java.time.Instant>()

    override suspend fun importActivity(activity: Activity, points: List<TrackPoint>): Long? {
        if (points.first().timestamp in timestamps) return null
        timestamps += points.map { it.timestamp }
        saved += activity
        return saved.size.toLong()
    }

    override fun observeFinishedActivities(type: ActivityType?): Flow<List<Activity>> = TODO()
    override fun observeActivity(id: Long): Flow<Activity?> = TODO()
    override suspend fun points(id: Long): List<TrackPoint> = TODO()
    override suspend fun updateDetails(id: Long, title: String?, notes: String?) = TODO()
    override suspend fun delete(id: Long) = TODO()
    override suspend fun backfillPreviews() = TODO()
}
