package com.gabrielifrim.dromos.core.data.mapper

import com.gabrielifrim.dromos.core.data.database.entity.TrackPointEntity
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import java.time.Instant

internal fun TrackPointEntity.toDomain(): TrackPoint = TrackPoint(
    timestamp = Instant.ofEpochMilli(timestampMillis),
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
    accuracyMeters = accuracyMeters,
    speedMetersPerSecond = speedMetersPerSecond,
    segment = segment,
    bearingDegrees = bearingDegrees,
    baroAltitudeMeters = baroAltitudeMeters,
)

internal fun TrackPoint.toEntity(activityId: Long): TrackPointEntity = TrackPointEntity(
    activityId = activityId,
    timestampMillis = timestamp.toEpochMilli(),
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
    accuracyMeters = accuracyMeters,
    speedMetersPerSecond = speedMetersPerSecond,
    segment = segment,
    bearingDegrees = bearingDegrees,
    baroAltitudeMeters = baroAltitudeMeters,
)
