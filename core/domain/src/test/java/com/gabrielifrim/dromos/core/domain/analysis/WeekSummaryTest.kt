package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class WeekSummaryTest {

    private val paris = ZoneId.of("Europe/Paris")

    private fun run(at: String, km: Double, gain: Double = 0.0) = Activity(
        id = at.hashCode().toLong(), type = ActivityType.RUN, status = ActivityStatus.FINISHED,
        startedAt = Instant.parse(at), endedAt = null, distanceMeters = km * 1000, movingDuration = Duration.ZERO,
        elevationGainMeters = gain, elevationLossMeters = 0.0, title = null, notes = null,
    )

    @Test
    fun `seules les sorties depuis lundi comptent`() {
        // Vendredi 9 octobre 2026 ; la semaine a commencé le lundi 5.
        val now = Instant.parse("2026-10-09T18:00:00Z")
        val summary = WeekSummary.of(
            listOf(
                run("2026-10-04T08:00:00Z", 10.0), // dimanche précédent
                run("2026-10-05T06:30:00Z", 5.0, gain = 40.0),
                run("2026-10-08T17:00:00Z", 8.0, gain = 100.0),
            ),
            now, paris,
        )
        assertThat(summary.count).isEqualTo(2)
        assertThat(summary.distanceMeters).isEqualTo(13_000.0)
        assertThat(summary.elevationGainMeters).isEqualTo(140.0)
        assertThat(summary.weekNumber).isEqualTo(41)
    }

    @Test
    fun `lundi juste après minuit, heure locale`() {
        // Dimanche 22 h 30 UTC = lundi 0 h 30 à Paris : la nouvelle semaine commence.
        val now = Instant.parse("2026-10-11T23:00:00Z")
        val summary = WeekSummary.of(listOf(run("2026-10-11T22:30:00Z", 3.0)), now, paris)
        assertThat(summary.count).isEqualTo(1)
    }
}
