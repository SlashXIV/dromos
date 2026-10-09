package com.gabrielifrim.dromos.core.data.repository

import com.gabrielifrim.dromos.core.data.dao.ActivityDao
import com.gabrielifrim.dromos.core.data.dao.TrackPointDao
import com.gabrielifrim.dromos.core.data.mapper.PreviewCodec
import com.gabrielifrim.dromos.core.data.mapper.toDomain
import com.gabrielifrim.dromos.core.domain.geo.TraceSimplifier
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
    private val trackPointDao: TrackPointDao,
) : ActivityRepository {

    override fun observeFinishedActivities(type: ActivityType?): Flow<List<Activity>> {
        val finished = ActivityStatus.FINISHED.name
        val rows = if (type == null) activityDao.observeByStatus(finished)
        else activityDao.observeByStatusAndType(finished, type.name)
        return rows.map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeActivity(id: Long): Flow<Activity?> =
        activityDao.observeById(id).map { it?.toDomain() }

    override suspend fun points(id: Long): List<TrackPoint> =
        trackPointDao.getForActivity(id).map { it.toDomain() }

    override suspend fun updateDetails(id: Long, title: String?, notes: String?) =
        activityDao.updateDetails(id, title?.trim()?.ifEmpty { null }, notes?.trim()?.ifEmpty { null })

    // Les points suivent par ON DELETE CASCADE.
    override suspend fun delete(id: Long) = activityDao.delete(id)

    override suspend fun backfillPreviews() {
        activityDao.idsWithoutPreview(ActivityStatus.FINISHED.name).forEach { id ->
            val points = trackPointDao.getForActivity(id).map { GeoPoint(it.latitude, it.longitude) }
            activityDao.updatePreview(id, PreviewCodec.encode(TraceSimplifier.simplify(points)))
        }
    }
}
