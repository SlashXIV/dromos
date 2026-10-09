package com.gabrielifrim.dromos.core.data.mapper

import com.gabrielifrim.dromos.core.data.database.entity.ActivityEntity
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import java.time.Duration
import java.time.Instant

internal fun ActivityEntity.toDomain(): Activity = Activity(
    id = id,
    type = ActivityType.valueOf(type),
    status = ActivityStatus.valueOf(status),
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    endedAt = endedAtMillis?.let(Instant::ofEpochMilli),
    distanceMeters = distanceMeters,
    movingDuration = Duration.ofMillis(movingDurationMillis),
    elevationGainMeters = elevationGainMeters,
    elevationLossMeters = elevationLossMeters,
    title = title,
    notes = notes,
    autoPause = autoPause,
    preview = PreviewCodec.decode(previewPolyline),
)

internal fun Activity.toEntity(): ActivityEntity = ActivityEntity(
    id = id,
    type = type.name,
    status = status.name,
    startedAtMillis = startedAt.toEpochMilli(),
    endedAtMillis = endedAt?.toEpochMilli(),
    distanceMeters = distanceMeters,
    movingDurationMillis = movingDuration.toMillis(),
    elevationGainMeters = elevationGainMeters,
    elevationLossMeters = elevationLossMeters,
    title = title,
    notes = notes,
    autoPause = autoPause,
    previewPolyline = preview?.let(PreviewCodec::encode),
)
