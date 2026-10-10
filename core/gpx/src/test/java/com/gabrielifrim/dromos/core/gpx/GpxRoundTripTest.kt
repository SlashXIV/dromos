package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.analysis.ActivityAnalyzer
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityStatus
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GpxRoundTripTest {

    @Test
    fun `aller-retour GPX - chaque point et chaque mesure reviennent à l'identique`() {
        val points = listOf(
            point(0, 48.8566, 2.3522, altitude = 35.2, accuracy = 4.5f, speed = 3.1f, bearing = 92.5f, baro = 0.0),
            point(1, 48.85662, 2.35224, altitude = null, accuracy = null, speed = null, bearing = null, baro = -0.4),
            // Coordonnées proches de zéro : jamais de notation « 1.0E-5 » dans le fichier.
            point(2, 0.00001, -0.0000123, altitude = -12.75, accuracy = 30f, speed = 0.1f, bearing = 0f, baro = null),
            point(3, -33.8688, 151.2093, altitude = 1.0, accuracy = 3f, speed = 2.9f, bearing = 359.9f, baro = 1.5, segment = 1),
        ).map { it.copy(timestamp = it.timestamp.plusMillis(250)) }
        val activity = activity(notes = "Fractionné & côtes <dur> « vent »")

        val track = GpxReader.read(gpx(activity, "Course du matin", points)).single()

        assertThat(track.points).isEqualTo(points)
        assertThat(track.name).isEqualTo("Course du matin")
        assertThat(track.description).isEqualTo(activity.notes)
        assertThat(track.type).isEqualTo(ActivityType.RUN)
    }

    @Test
    fun `aller-retour GPX - une sortie réimportée retombe sur les mêmes totaux`() {
        val points = syntheticRun(seconds = 1_200, speed = 3.2)
        val original = ActivityAnalyzer.totals(points, ActivityType.RUN, autoPause = true)

        val reread = GpxReader.read(gpx(activity(), "Sortie", points)).single().points

        assertThat(ActivityAnalyzer.totals(reread, ActivityType.RUN, autoPause = true)).isEqualTo(original)
        assertThat(original.distanceMeters).isWithin(100.0).of(3_840.0)
    }

    @Test
    fun `lit un GPX étranger - préfixes, extensions Garmin, fuseaux et plusieurs traces`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <g:gpx version="1.1" creator="Garmin" xmlns:g="http://www.topografix.com/GPX/1/1"
                   xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v2">
              <g:metadata><g:name>Fichier</g:name></g:metadata>
              <g:wpt lat="1" lon="1"><g:name>Point d’eau</g:name></g:wpt>
              <g:trk>
                <g:name>Footing</g:name><g:type>running</g:type>
                <g:trkseg>
                  <g:trkpt lat="45.1" lon="5.7"><g:ele>210.5</g:ele><g:time>2026-10-10T10:00:00+02:00</g:time>
                    <g:extensions><gpxtpx:TrackPointExtension><gpxtpx:speed>3.4</gpxtpx:speed><gpxtpx:course>180</gpxtpx:course></gpxtpx:TrackPointExtension></g:extensions>
                  </g:trkpt>
                  <g:trkpt lat="45.10003" lon="5.7"><g:time>2026-10-10T08:00:01.500Z</g:time></g:trkpt>
                  <g:trkpt lat="45.10006" lon="5.7"><g:ele>211</g:ele></g:trkpt>
                </g:trkseg>
                <g:trkseg><g:trkpt lat="45.2" lon="5.7"><g:time>2026-10-10T08:10:00</g:time></g:trkpt></g:trkseg>
              </g:trk>
              <g:trk><g:type>Hiking</g:type><g:trkseg><g:trkpt lat="1" lon="2"><g:time>2026-10-11T08:00:00Z</g:time></g:trkpt></g:trkseg></g:trk>
            </g:gpx>
        """.trimIndent()

        val tracks = GpxReader.read(xml.byteInputStream())

        assertThat(tracks).hasSize(2)
        val first = tracks[0]
        assertThat(first.name).isEqualTo("Footing")
        assertThat(first.type).isEqualTo(ActivityType.RUN)
        // Le point sans horodatage est écarté ; les fuseaux sont ramenés en UTC.
        assertThat(first.points.map { it.timestamp }).containsExactly(
            Instant.parse("2026-10-10T08:00:00Z"),
            Instant.parse("2026-10-10T08:00:01.500Z"),
            Instant.parse("2026-10-10T08:10:00Z"),
        ).inOrder()
        assertThat(first.points.map { it.segment }).containsExactly(0, 0, 1).inOrder()
        with(first.points[0]) {
            assertThat(altitudeMeters).isEqualTo(210.5)
            assertThat(speedMetersPerSecond).isEqualTo(3.4f)
            assertThat(bearingDegrees).isEqualTo(180f)
            assertThat(accuracyMeters).isNull()
        }
        // Sans nom de trace, celui du fichier.
        assertThat(tracks[1].name).isEqualTo("Fichier")
        assertThat(tracks[1].type).isEqualTo(ActivityType.WALK)
    }

    @Test
    fun `un fichier piégé n'a accès à aucune entité externe`() = kotlinx.coroutines.test.runTest {
        val secret = kotlin.io.path.createTempFile().toFile().apply { writeText("SECRET"); deleteOnExit() }
        val xml = """
            <?xml version="1.0"?>
            <!DOCTYPE gpx [<!ENTITY xxe SYSTEM "${secret.toURI()}">]>
            <gpx xmlns="http://www.topografix.com/GPX/1/1"><trk><name>&xxe;</name><trkseg>
              <trkpt lat="1" lon="1"><time>2026-10-10T08:00:00Z</time></trkpt>
              <trkpt lat="1.0001" lon="1"><time>2026-10-10T08:00:10Z</time></trkpt>
            </trkseg></trk></gpx>
        """.trimIndent()
        val repository = FakeActivityRepository()

        val result = GpxImporter(repository).import(xml.byteInputStream())

        assertThat(result).isEqualTo(ImportResult(rejected = 1))
        assertThat(repository.saved).isEmpty()
    }

    @Test
    fun `import - doublons, traces sans temps et fichiers illisibles sont comptés à part`() = kotlinx.coroutines.test.runTest {
        val repository = FakeActivityRepository()
        val importer = GpxImporter(repository)
        val file = gpx(activity(), "Sortie", syntheticRun(seconds = 300, speed = 3.0)).readBytes()

        assertThat(importer.import(ByteArrayInputStream(file))).isEqualTo(ImportResult(imported = 1))
        assertThat(importer.import(ByteArrayInputStream(file))).isEqualTo(ImportResult(duplicates = 1))
        val untimed = """<gpx><trk><trkseg><trkpt lat="1" lon="1"/><trkpt lat="2" lon="2"/></trkseg></trk></gpx>"""
        assertThat(importer.import(untimed.byteInputStream())).isEqualTo(ImportResult(rejected = 1))
        assertThat(importer.import("pas du XML".byteInputStream())).isEqualTo(ImportResult(rejected = 1))

        with(repository.saved.single()) {
            assertThat(status).isEqualTo(ActivityStatus.FINISHED)
            assertThat(type).isEqualTo(ActivityType.RUN)
            assertThat(title).isEqualTo("Sortie")
            assertThat(distanceMeters).isWithin(30.0).of(900.0)
            assertThat(movingDuration.seconds).isIn(290L..300L)
        }
    }

    @Test
    fun `import - sans type dans le fichier, la vitesse décide`() = kotlinx.coroutines.test.runTest {
        val repository = FakeActivityRepository()
        val importer = GpxImporter(repository)

        importer.import(untypedGpx(syntheticRun(seconds = 300, speed = 3.0)))
        importer.import(untypedGpx(syntheticRun(seconds = 300, speed = 1.4, start = START.plusSeconds(3_600))))

        assertThat(repository.saved.map { it.type }).containsExactly(ActivityType.RUN, ActivityType.WALK).inOrder()
    }

    @Test
    fun `KML - une ligne par segment, coordonnées longitude d'abord`() {
        val points = syntheticRun(seconds = 60, speed = 3.0) +
            syntheticRun(seconds = 30, speed = 3.0, start = START.plusSeconds(120)).map { it.copy(segment = 1) }
        val out = StringBuilder().also { KmlWriter.write(activity(notes = "a & b"), "Sortie", points, it) }

        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(out.toString().byteInputStream())
        val lines = document.getElementsByTagName("coordinates")

        assertThat(lines.length).isEqualTo(2)
        val first = (lines.item(0) as Element).textContent.trim().split(" ")
        assertThat(first).hasSize(61)
        assertThat(first[0]).startsWith("${points[0].longitude},${points[0].latitude}")
    }

    private fun gpx(activity: Activity, name: String, points: List<TrackPoint>) =
        StringBuilder().also { GpxWriter.write(activity, name, points, it) }.toString().byteInputStream()

    private fun untypedGpx(points: List<TrackPoint>) =
        gpx(activity(), "x", points).readBytes().decodeToString().replace(Regex("<type>.*</type>"), "").byteInputStream()

    private fun activity(notes: String? = null) = Activity(
        id = 1, type = ActivityType.RUN, status = ActivityStatus.FINISHED, startedAt = START, endedAt = null,
        distanceMeters = 0.0, movingDuration = Duration.ZERO, elevationGainMeters = 0.0, elevationLossMeters = 0.0,
        title = null, notes = notes,
    )

    private fun point(
        second: Long, lat: Double, lon: Double, altitude: Double?, accuracy: Float?, speed: Float?,
        bearing: Float?, baro: Double?, segment: Int = 0,
    ) = TrackPoint(START.plusSeconds(second), lat, lon, altitude, accuracy, speed, segment, bearing, baro)

    /** Course en ligne droite vers le nord-est, une position par seconde, avec bruit GPS. */
    private fun syntheticRun(seconds: Int, speed: Double, start: Instant = START): List<TrackPoint> {
        val random = Random(42)
        val heading = Math.toRadians(45.0)
        return (0..seconds).map { s ->
            val meters = s * speed
            val north = meters * cos(heading) + random.nextDouble(-2.0, 2.0)
            val east = meters * sin(heading) + random.nextDouble(-2.0, 2.0)
            TrackPoint(
                timestamp = start.plusSeconds(s.toLong()),
                latitude = 45.0 + north / 111_195.0,
                longitude = 5.0 + east / (111_195.0 * cos(Math.toRadians(45.0))),
                altitudeMeters = 200.0 + 10 * sin(s / 60.0) + random.nextDouble(-1.0, 1.0),
                accuracyMeters = 4f,
                speedMetersPerSecond = (speed + random.nextDouble(-0.1, 0.1)).toFloat(),
                segment = 0,
                bearingDegrees = 45f,
            )
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-10-10T06:30:00Z")
    }
}
