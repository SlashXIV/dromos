package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.parsers.SAXParserFactory

/** Une trace (`trk`) lue dans un fichier GPX. */
data class GpxTrack(
    val name: String?,
    val description: String?,
    /** null si le fichier ne le dit pas, ou dans un vocabulaire inconnu. */
    val type: ActivityType?,
    /** Points horodatés seulement : sans temps, ni allure ni durée. */
    val points: List<TrackPoint>,
)

/**
 * Lecteur GPX 1.0 / 1.1 en flux (SAX) : un fichier de plusieurs heures ne charge
 * jamais son arbre en mémoire. Tolérant sur le vocabulaire : les éléments sont
 * reconnus par leur nom local, quel que soit le préfixe, et la vitesse et le cap
 * sont lus aussi dans les extensions Garmin.
 */
object GpxReader {

    /** @throws org.xml.sax.SAXException si le fichier n'est pas du XML lisible. */
    fun read(input: InputStream): List<GpxTrack> {
        val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
        // Un fichier reçu ne doit rien pouvoir lire ni télécharger (XXE) : pas de
        // DOCTYPE, et aucune entité externe résolue. Selon l'analyseur (JDK, Expat
        // d'Android), toutes ces options ne sont pas reconnues.
        listOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
        ).forEach { (feature, value) -> runCatching { factory.setFeature(feature, value) } }
        val handler = Handler()
        factory.newSAXParser().xmlReader.apply {
            entityResolver = EntityResolver { _, _ -> InputSource(StringReader("")) }
            contentHandler = handler
            parse(InputSource(input))
        }
        return handler.tracks
    }

    private class Handler : DefaultHandler() {
        val tracks = mutableListOf<GpxTrack>()

        private val path = ArrayDeque<String>()
        private val text = StringBuilder()
        private var fileName: String? = null

        private var trackName: String? = null
        private var trackDescription: String? = null
        private var trackType: ActivityType? = null
        private var points = mutableListOf<TrackPoint>()
        private var segment = -1

        private var inPoint = false
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var elevation: Double? = null
        private var time: Instant? = null
        private var speed: Float? = null
        private var course: Float? = null
        private var accuracy: Float? = null
        private var baro: Double? = null

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            text.setLength(0)
            when (localName) {
                "trk" -> {
                    trackName = null; trackDescription = null; trackType = null
                    points = mutableListOf(); segment = -1
                }
                "trkseg" -> segment++
                "trkpt" -> {
                    inPoint = true
                    latitude = attributes.getValue("lat")?.trim()?.toDoubleOrNull()
                    longitude = attributes.getValue("lon")?.trim()?.toDoubleOrNull()
                    elevation = null; time = null; speed = null; course = null; accuracy = null; baro = null
                }
            }
            path.addLast(localName)
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.append(ch, start, length)
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            path.removeLast()
            val parent = path.lastOrNull()
            val value = text.toString().trim()
            text.setLength(0)
            if (inPoint) {
                when (localName) {
                    "ele" -> elevation = value.toDoubleOrNull()
                    "time" -> time = parseTime(value)
                    "speed" -> speed = value.toFloatOrNull()
                    "course" -> course = value.toFloatOrNull()
                    "accuracy" -> if (uri == GpxWriter.DROMOS_NAMESPACE) accuracy = value.toFloatOrNull()
                    "baro" -> if (uri == GpxWriter.DROMOS_NAMESPACE) baro = value.toDoubleOrNull()
                    "trkpt" -> endPoint()
                }
                return
            }
            when {
                localName == "name" && parent == "metadata" -> fileName = value.ifEmpty { null }
                localName == "name" && parent == "trk" -> trackName = value.ifEmpty { null }
                localName == "desc" && parent == "trk" -> trackDescription = value.ifEmpty { null }
                localName == "type" && parent == "trk" -> trackType = parseType(value)
                localName == "trk" -> tracks += GpxTrack(trackName ?: fileName, trackDescription, trackType, points)
            }
        }

        private fun endPoint() {
            inPoint = false
            val lat = latitude ?: return
            val lon = longitude ?: return
            val timestamp = time ?: return
            points += TrackPoint(
                timestamp = timestamp,
                latitude = lat,
                longitude = lon,
                altitudeMeters = elevation,
                accuracyMeters = accuracy,
                speedMetersPerSecond = speed,
                segment = segment.coerceAtLeast(0),
                bearingDegrees = course,
                baroAltitudeMeters = baro,
            )
        }
    }

    /** ISO 8601, avec ou sans fuseau (sans fuseau : UTC, comme le veut GPX). */
    private fun parseTime(value: String): Instant? =
        runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
            .getOrNull()

    /** Vocabulaires de Garmin (« running »), Strava (codes numériques) et des autres. */
    private fun parseType(value: String): ActivityType? {
        val type = value.lowercase()
        return when {
            "run" in type || type == "9" -> ActivityType.RUN
            "walk" in type || "hik" in type || type == "10" || type == "4" -> ActivityType.WALK
            else -> null
        }
    }
}
