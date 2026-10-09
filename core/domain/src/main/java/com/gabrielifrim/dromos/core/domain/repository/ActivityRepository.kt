package com.gabrielifrim.dromos.core.domain.repository

import com.gabrielifrim.dromos.core.domain.model.Activity
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    /** Sorties terminées, la plus récente d'abord. */
    fun observeFinishedActivities(): Flow<List<Activity>>
}
