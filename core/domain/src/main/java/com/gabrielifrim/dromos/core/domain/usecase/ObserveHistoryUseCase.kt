package com.gabrielifrim.dromos.core.domain.usecase

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHistoryUseCase @Inject constructor(
    private val repository: ActivityRepository,
) {
    operator fun invoke(): Flow<List<Activity>> = repository.observeFinishedActivities()
}
