package com.gabrielifrim.dromos.core.data.repository

import androidx.room.withTransaction
import com.gabrielifrim.dromos.core.data.dao.ActivityDao
import com.gabrielifrim.dromos.core.data.dao.TrackPointDao
import com.gabrielifrim.dromos.core.data.database.DromosDatabase
import com.gabrielifrim.dromos.core.data.database.entity.ActivityEntity
import com.gabrielifrim.dromos.core.data.mapper.toDomain
import com.gabrielifrim.dromos.core.data.mapper.toEntity
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.TrackingRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

internal class TrackingRepositoryImpl @Inject constructor(
    private val database: DromosDatabase,
    private val activityDao: ActivityDao,
    private val trackPointDao: TrackPointDao,
) : TrackingRepository {

    override suspend fun createActivity(type: ActivityType, startedAt: Instant): Long =
        activityDao.insert(
            ActivityEntity(
                type = type.name,
                status = ActivityStatus.IN_PROGRESS.name,
                startedAtMillis = startedAt.toEpochMilli(),
                endedAtMillis = null,
                distanceMeters = 0.0,
                movingDurationMillis = 0,
                elevationGainMeters = 0.0,
                elevationLossMeters = 0.0,
                title = null,
                notes = null,
            ),
        )

    override suspend fun appendPoints(
        activityId: Long,
        points: List<TrackPoint>,
        distanceMeters: Double,
        movingDuration: Duration,
    ) {
        // Une seule transaction : après une mort du processus, les totaux
        // correspondent toujours exactement aux points présents.
        database.withTransaction {
            if (points.isNotEmpty()) trackPointDao.insertAll(points.map { it.toEntity(activityId) })
            activityDao.updateProgress(activityId, distanceMeters, movingDuration.toMillis())
        }
    }

    override suspend fun finishActivity(
        activityId: Long,
        endedAt: Instant,
        distanceMeters: Double,
        movingDuration: Duration,
    ) = activityDao.finish(
        id = activityId,
        status = ActivityStatus.FINISHED.name,
        endedAtMillis = endedAt.toEpochMilli(),
        distanceMeters = distanceMeters,
        movingDurationMillis = movingDuration.toMillis(),
    )

    override suspend fun deleteActivity(activityId: Long) = activityDao.delete(activityId)

    override suspend fun findInProgress(): Activity? =
        activityDao.findLatestByStatus(ActivityStatus.IN_PROGRESS.name)?.toDomain()

    override suspend fun points(activityId: Long): List<TrackPoint> =
        trackPointDao.getForActivity(activityId).map { it.toDomain() }
}
