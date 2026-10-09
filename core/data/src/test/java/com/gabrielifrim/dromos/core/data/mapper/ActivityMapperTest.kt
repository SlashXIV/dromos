package com.gabrielifrim.dromos.core.data.mapper

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ActivityMapperTest {

    @Test
    fun `une sortie survit à l'aller-retour entité - domaine`() {
        val activity = Activity(
            id = 42,
            type = ActivityType.RUN,
            status = ActivityStatus.FINISHED,
            startedAt = Instant.ofEpochMilli(1_700_000_000_000),
            endedAt = Instant.ofEpochMilli(1_700_003_600_000),
            distanceMeters = 10_012.5,
            movingDuration = Duration.ofMinutes(52),
            elevationGainMeters = 84.0,
            elevationLossMeters = 81.5,
            title = "Sortie du dimanche",
            notes = null,
        )
        assertThat(activity.toEntity().toDomain()).isEqualTo(activity)
    }

    @Test
    fun `les énumérations sont stockées par leur nom`() {
        val entity = Activity(
            id = 1, type = ActivityType.WALK, status = ActivityStatus.IN_PROGRESS,
            startedAt = Instant.EPOCH, endedAt = null, distanceMeters = 0.0,
            movingDuration = Duration.ZERO, elevationGainMeters = 0.0, elevationLossMeters = 0.0,
            title = null, notes = null,
        ).toEntity()
        assertThat(entity.type).isEqualTo("WALK")
        assertThat(entity.status).isEqualTo("IN_PROGRESS")
    }
}
