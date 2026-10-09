package com.gabrielifrim.dromos.core.data.repository

import com.gabrielifrim.dromos.core.data.dao.ActivityDao
import com.gabrielifrim.dromos.core.data.mapper.toDomain
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
) : ActivityRepository {

    override fun observeFinishedActivities(): Flow<List<Activity>> =
        activityDao.observeByStatus(ActivityStatus.FINISHED.name)
            .map { entities -> entities.map { it.toDomain() } }
}
