package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.location.BarometerSource
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.domain.model.LocationSample
import com.gabrielifrim.dromos.core.domain.model.MapPreferences
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.domain.model.TrackTotals
import com.gabrielifrim.dromos.core.domain.model.TrackingPreferences
import com.gabrielifrim.dromos.core.domain.repository.SettingsRepository
import com.gabrielifrim.dromos.core.domain.repository.TrackingRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

    private class FakeBarometer : BarometerSource {
        override fun altitudes(): Flow<Double> = emptyFlow()
    }

    private class FakeSettings(autoPause: Boolean) : SettingsRepository {
        val prefs = MutableStateFlow(TrackingPreferences(autoPause = autoPause))
        override val appearance: Flow<AppearanceSettings> = MutableStateFlow(AppearanceSettings())
        override suspend fun setPalette(paletteId: String) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override val tracking: Flow<TrackingPreferences> = prefs
        override suspend fun setAutoPause(enabled: Boolean) { prefs.value = TrackingPreferences(enabled) }
        override suspend fun setBatteryPromptDismissed(dismissed: Boolean) = Unit
        override val map: Flow<MapPreferences> = MutableStateFlow(MapPreferences())
        override suspend fun setOnlineBasemap(enabled: Boolean) = Unit
    }

    private class FakeRepository : TrackingRepository {
        val activities = mutableMapOf<Long, Activity>()
        val points = mutableMapOf<Long, MutableList<TrackPoint>>()
        private var nextId = 1L

        override suspend fun createActivity(type: ActivityType, startedAt: Instant, autoPause: Boolean): Long {
            val id = nextId++
            activities[id] = Activity(
                id, type, ActivityStatus.IN_PROGRESS, startedAt, null, 0.0, Duration.ZERO, 0.0, 0.0, null, null, autoPause,
            )
            points[id] = mutableListOf()
            return id
        }

        override suspend fun appendPoints(activityId: Long, points: List<TrackPoint>, totals: TrackTotals) {
            this.points.getValue(activityId) += points
            activities[activityId] = activities.getValue(activityId).withTotals(totals)
        }

        override suspend fun finishActivity(activityId: Long, endedAt: Instant, totals: TrackTotals) {
            activities[activityId] = activities.getValue(activityId).withTotals(totals)
                .copy(status = ActivityStatus.FINISHED, endedAt = endedAt)
        }

        private fun Activity.withTotals(t: TrackTotals) = copy(
            distanceMeters = t.distanceMeters, movingDuration = t.movingDuration,
            elevationGainMeters = t.elevationGainMeters, elevationLossMeters = t.elevationLossMeters,
        )

        override suspend fun deleteActivity(activityId: Long) {
            activities.remove(activityId); points.remove(activityId)
        }

        override suspend fun findInProgress(): Activity? = activities.values.firstOrNull { it.status == ActivityStatus.IN_PROGRESS }

        override suspend fun points(activityId: Long): List<TrackPoint> = points[activityId].orEmpty()
    }

    private val clock = FakeClock()
    private val source = FakeLocationSource()
    private val repo = FakeRepository()

    private fun TestScope.engine(autoPause: Boolean = false) =
        TrackingEngine(source, FakeBarometer(), repo, FakeSettings(autoPause), clock, backgroundScope).also { runCurrent() }

    /** Position courante du coureur, en mètres vers le nord. */
    private var north = 0.0
    private val degPerMeter = 1 / 111_195.0

    /** [seconds] secondes à [speed] m/s, une position par seconde. */
    private suspend fun TestScope.move(seconds: Int, speed: Double) = repeat(seconds) {
        north += speed
        source.samples.emit(LocationSample(clock.instant(), 48.0 + north * degPerMeter, 2.0, 35.0, 4f))
        runCurrent()
        clock.advance(1)
    }

    private val TrackingEngine.active get() = state.value as TrackingState.Active

    @Test
    fun `une sortie complète est enregistrée avec sa distance et sa durée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(120, 3.0)

        val id = engine.stop()

        assertThat(id).isEqualTo(1L)
        assertThat(engine.state.value).isEqualTo(TrackingState.Idle)
        val saved = repo.activities.getValue(1L)
        assertThat(saved.status).isEqualTo(ActivityStatus.FINISHED)
        assertThat(saved.distanceMeters).isWithin(10.0).of(357.0)
        assertThat(saved.movingDuration).isEqualTo(Duration.ofSeconds(120))
        assertThat(repo.points.getValue(1L)).hasSize(120)
    }

    @Test
    fun `la pause arrête le chrono et la reprise ouvre un nouveau segment`() = runTest {
        val engine = engine()
        engine.start(ActivityType.WALK); runCurrent()
        move(60, 1.5)
        engine.pause()
        clock.advance(600)
        north += 1_000 // dix minutes de pause, 1 km parcouru sans suivi : rien ne doit être compté
        engine.resume(); runCurrent()
        move(60, 1.5)

        assertThat(engine.active.segment).isEqualTo(1)
        assertThat(engine.active.movingDurationAt(clock.instant())).isEqualTo(Duration.ofSeconds(120))
        assertThat(engine.active.distanceMeters).isWithin(12.0).of(177.0)
    }

    @Test
    fun `aucune position n'est prise en pause`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(1, 3.0)
        engine.pause(); runCurrent()
        move(5, 3.0)
        assertThat(engine.active.pointCount).isEqualTo(1)
    }

    @Test
    fun `les points sont écrits en base au fil de l'eau`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(TrackingEngine.FLUSH_EVERY_POINTS, 3.0)
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
    fun `une sortie interrompue est récupérée en pause avec les mêmes mesures`() = runTest {
        val first = engine()
        first.start(ActivityType.RUN); runCurrent()
        move(100, 3.0)
        // Référence : la même trace mise en pause proprement (segment clos).
        first.pause()
        val expected = first.active.distanceMeters
        // Le processus meurt : un nouveau moteur repart de la base.
        val second = engine()

        assertThat(second.recover()).isTrue()

        val state = second.active
        assertThat(state.activityId).isEqualTo(1L)
        assertThat(state.paused).isTrue()
        assertThat(state.recovered).isTrue()
        assertThat(state.pointCount).isEqualTo(100)
        // Recalculée depuis les points bruts : exactement la même valeur.
        assertThat(state.distanceMeters).isEqualTo(expected)
    }

    @Test
    fun `rien à récupérer quand aucune sortie n'est en cours`() = runTest {
        assertThat(engine().recover()).isFalse()
    }

    @Test
    fun `une ancienne position en cache est ignorée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        source.samples.emit(LocationSample(clock.instant().minusSeconds(3600), 37.42, -122.08)); runCurrent()
        move(1, 3.0)
        assertThat(engine.active.pointCount).isEqualTo(1)
        assertThat(engine.active.distanceMeters).isEqualTo(0.0)
    }

    @Test
    fun `l'allure affichée vient de la vitesse lissée`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(30, 3.0)
        assertThat(engine.active.currentSpeedMetersPerSecond!!.toDouble()).isWithin(0.2).of(3.0)
    }

    @Test
    fun `la pause automatique retire l'arrêt du temps en mouvement`() = runTest {
        val engine = engine(autoPause = true)
        engine.start(ActivityType.RUN); runCurrent()
        move(60, 3.0)
        move(60, 0.0) // feu rouge d'une minute
        assertThat(engine.active.autoPaused).isTrue()
        move(60, 3.0)
        assertThat(engine.active.autoPaused).isFalse()

        val moving = engine.active.movingDurationAt(clock.instant())
        // 120 s de course ; la détection de l'arrêt et la décélération du filtre coûtent quelques secondes.
        assertThat(moving.seconds).isIn(115L..135L)
    }

    @Test
    fun `sans pause automatique, l'arrêt reste compté`() = runTest {
        val engine = engine(autoPause = false)
        engine.start(ActivityType.RUN); runCurrent()
        move(60, 3.0)
        move(60, 0.0)
        assertThat(engine.active.autoPaused).isFalse()
        assertThat(engine.active.movingDurationAt(clock.instant())).isEqualTo(Duration.ofSeconds(120))
    }

    @Test
    fun `le tracé suit les points et se coupe à chaque pause`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(10, 3.0)
        engine.pause()
        north += 500
        engine.resume(); runCurrent()
        move(5, 3.0)

        val trace = engine.trace.value
        assertThat(trace.map { it.size }).containsExactly(10, 5).inOrder()
        // Le premier point du second segment est 500 m plus au nord : aucun trait ne les relie.
        assertThat(trace[1].first().latitude).isGreaterThan(trace[0].last().latitude + 400 * degPerMeter)
    }

    @Test
    fun `le tracé est reconstitué à la récupération et vidé à la fin`() = runTest {
        val first = engine()
        first.start(ActivityType.RUN); runCurrent()
        move(10, 3.0)
        val second = engine()
        second.recover()
        assertThat(second.trace.value.single()).hasSize(10)

        second.stop()
        assertThat(second.trace.value).isEmpty()
    }

    @Test
    fun `une source GPS en échec est signalée sans casser la sortie`() = runTest {
        source.failing = true
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        assertThat(engine.active.locationError).isTrue()
    }

    @Test
    fun `abandonner supprime la sortie`() = runTest {
        val engine = engine()
        engine.start(ActivityType.RUN); runCurrent()
        move(1, 3.0)
        engine.discard()
        assertThat(engine.state.value).isEqualTo(TrackingState.Idle)
        assertThat(repo.activities).isEmpty()
    }
}
