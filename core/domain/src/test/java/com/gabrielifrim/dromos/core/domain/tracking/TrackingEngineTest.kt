package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.LocationSample
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.repository.TrackingRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingEngineTest {

    private class FakeClock(var now: Instant = Instant.parse("2026-10-10T08:00:00Z")) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        fun advance(seconds: Long) { now = now.plusSeconds(seconds) }
    }

    private class FakeLocationSource : LocationSource {
        val samples = MutableSharedFlow<LocationSample>()
        var failing = false
        override fun locations(): Flow<LocationSample> =
            if (failing) flow { throw SecurityException("permission retirée") } else samples
    }

    private class FakeRepository : TrackingRepository {
        val activities = mutableMapOf<Long, Activity>()
        val points = mutableMapOf<Long, MutableList<TrackPoint>>()
        private var nextId = 1L

        override suspend fun createActivity(type: ActivityType, startedAt: Instant): Long {
            val id = nextId++
            activities[id] = Activity(id, type, ActivityStatus.IN_PROGRESS, startedAt, null, 0.0, Duration.ZERO, 0.0, 0.0, null, null)
            points[id] = mutableListOf()
            return id
        }

        override suspend fun appendPoints(activityId: Long, points: List<TrackPoint>, distanceMeters: Double, movingDuration: Duration) {
            this.points.getValue(activityId) += points
            activities[activityId] = activities.getValue(activityId).copy(distanceMeters = distanceMeters, movingDuration = movingDuration)
        }

        override suspend fun finishActivity(activityId: Long, endedAt: Instant, distanceMeters: Double, movingDuration: Duration) {
            activities[activityId] = activities.getValue(activityId).copy(
                status = ActivityStatus.FINISHED, endedAt = endedAt, distanceMeters = distanceMeters, movingDuration = movingDuration,
            )
        }

        override suspend fun deleteActivity(activityId: Long) {
            activities.remove(activityId); points.remove(activityId)
        }

        override suspend fun findInProgress(): Activity? = activities.values.firstOrNull { it.status == ActivityStatus.IN_PROGRESS }

        override suspend fun points(activityId: Long): List<TrackPoint> = points[activityId].orEmpty()
    }

    private val clock = FakeClock()
    private val source = FakeLocationSource()
    private val repo = FakeRepository()

    private fun TestScope.engine() = TrackingEngine(source, repo, clock, backgroundScope)

    private fun sample(lat: Double) = LocationSample(clock.instant(), lat, 2.0, 35.0, 4f, 3f)

    private suspend fun TestScope.emit(lat: Double) {
        source.samples.emit(sample(lat)); runCurrent()
    }

    @Test
    fun `une sortie complète est enregistrée avec sa distance et sa durée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        emit(48.000); clock.advance(30); emit(48.001); clock.advance(30); emit(48.002)

        val id = engine.stop()

        assertThat(id).isEqualTo(1L)
        assertThat(engine.state.value).isEqualTo(TrackingState.Idle)
        val saved = repo.activities.getValue(1L)
        assertThat(saved.status).isEqualTo(ActivityStatus.FINISHED)
        assertThat(saved.distanceMeters).isWithin(0.5).of(222.4)
        assertThat(saved.movingDuration).isEqualTo(Duration.ofSeconds(60))
        assertThat(repo.points.getValue(1L)).hasSize(3)
    }

    @Test
    fun `la pause arrête le chrono et la reprise ouvre un nouveau segment`() = runTest {
        val engine = engine()
        engine.start(ActivityType.WALK); runCurrent()
        emit(48.000); clock.advance(60); emit(48.001)
        engine.pause()
        clock.advance(600) // dix minutes de pause : ni durée, ni distance
        engine.resume(); runCurrent()
        emit(48.010); clock.advance(60); emit(48.011)

        val active = engine.state.value as TrackingState.Active
        assertThat(active.segment).isEqualTo(1)
        assertThat(active.movingDurationAt(clock.instant())).isEqualTo(Duration.ofSeconds(120))
        assertThat(active.distanceMeters).isWithin(0.5).of(222.4)
    }

    @Test
    fun `aucune position n'est prise en pause`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        emit(48.000)
        engine.pause(); runCurrent()
        emit(48.5)
        assertThat((engine.state.value as TrackingState.Active).pointCount).isEqualTo(1)
    }

    @Test
    fun `les points sont écrits en base au fil de l'eau`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        repeat(TrackingEngine.FLUSH_EVERY_POINTS) { emit(48.0 + it * 0.0001); clock.advance(1) }
        assertThat(repo.points.getValue(1L)).hasSize(TrackingEngine.FLUSH_EVERY_POINTS)
        assertThat(repo.activities.getValue(1L).status).isEqualTo(ActivityStatus.IN_PROGRESS)
    }

    @Test
    fun `une sortie sans aucun point est supprimée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        assertThat(engine.stop()).isNull()
        assertThat(repo.activities).isEmpty()
    }

    @Test
    fun `une sortie interrompue est récupérée en pause`() = runTest {
        val first = engine()
        first.start(ActivityType.RUN); runCurrent()
        repeat(5) { emit(48.0 + it * 0.001); clock.advance(10) }
        // Le processus meurt : un nouveau moteur repart de la base.
        val second = engine()

        assertThat(second.recover()).isTrue()

        val state = second.state.value as TrackingState.Active
        assertThat(state.activityId).isEqualTo(1L)
        assertThat(state.paused).isTrue()
        assertThat(state.recovered).isTrue()
        assertThat(state.distanceMeters).isWithin(1.0).of(444.8)
        assertThat(state.pointCount).isEqualTo(5)
    }

    @Test
    fun `une ancienne position en cache est ignorée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        source.samples.emit(LocationSample(clock.instant().minusSeconds(3600), 37.42, -122.08)); runCurrent()
        emit(48.000)
        val state = engine.state.value as TrackingState.Active
        assertThat(state.pointCount).isEqualTo(1)
        assertThat(state.distanceMeters).isEqualTo(0.0)
    }

    @Test
    fun `sans vitesse du récepteur, l'allure instantanée vient des points`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        source.samples.emit(LocationSample(clock.instant(), 48.000, 2.0)); runCurrent()
        clock.advance(1)
        // Vitesse annoncée nulle alors qu'on avance : récepteur qui ne sait pas, pas un arrêt.
        source.samples.emit(LocationSample(clock.instant(), 48.00003, 2.0, speedMetersPerSecond = 0f)); runCurrent()
        val speed = (engine.state.value as TrackingState.Active).currentSpeedMetersPerSecond
        assertThat(speed!!.toDouble()).isWithin(0.05).of(3.34)
    }

    @Test
    fun `rien à récupérer quand aucune sortie n'est en cours`() = runTest {
        assertThat(engine().recover()).isFalse()
    }

    @Test
    fun `une source GPS en échec est signalée sans casser la sortie`() = runTest {
        source.failing = true
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        assertThat((engine.state.value as TrackingState.Active).locationError).isTrue()
    }

    @Test
    fun `abandonner supprime la sortie`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        emit(48.0)
        engine.discard()
        assertThat(engine.state.value).isEqualTo(TrackingState.Idle)
        assertThat(repo.activities).isEmpty()
    }
}
