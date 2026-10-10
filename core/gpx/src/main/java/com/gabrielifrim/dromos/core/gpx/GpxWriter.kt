package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.model.TrackPoint

/**
 * GPX 1.1 d'une sortie : un `trkseg` par segment (reprise après pause manuelle).
 *
 * Les mesures brutes que GPX ne prévoit pas (vitesse Doppler, cap, précision,
 * altitude barométrique) vont dans des extensions Dromos : réimporté, le fichier
 * redonne exactement les mêmes points, donc les mêmes distance et dénivelé.
 * Les autres applications ignorent ces extensions.
 */
object GpxWriter {

    const val NAMESPACE = "http://www.topografix.com/GPX/1/1"
    const val DROMOS_NAMESPACE = "https://github.com/SlashXIV/dromos/gpx/1"

    fun write(activity: Activity, name: String, points: List<TrackPoint>, out: Appendable) {
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        out.append("<gpx version=\"1.1\" creator=\"Dromos\" xmlns=\"$NAMESPACE\" xmlns:dromos=\"$DROMOS_NAMESPACE\">\n")
        out.append("  <metadata><name>").append(name.escapeXml()).append("</name><time>")
            .append(activity.startedAt.toString()).append("</time></metadata>\n")
        out.append("  <trk>\n    <name>").append(name.escapeXml()).append("</name>\n")
        activity.notes?.let { out.append("    <desc>").append(it.escapeXml()).append("</desc>\n") }
        out.append("    <type>").append(typeName(activity.type)).append("</type>\n")
        points.groupBy { it.segment }.toSortedMap().values.forEach { segment ->
            out.append("    <trkseg>\n")
            segment.forEach { writePoint(it, out) }
            out.append("    </trkseg>\n")
        }
        out.append("  </trk>\n</gpx>\n")
    }

    private fun writePoint(point: TrackPoint, out: Appendable) {
        out.append("      <trkpt lat=\"").append(point.latitude.plain())
            .append("\" lon=\"").append(point.longitude.plain()).append("\">")
        point.altitudeMeters?.let { out.append("<ele>").append(it.plain()).append("</ele>") }
        out.append("<time>").append(point.timestamp.toString()).append("</time>")
        val extensions = listOfNotNull(
            point.speedMetersPerSecond?.let { "speed" to it.plain() },
            point.bearingDegrees?.let { "course" to it.plain() },
            point.accuracyMeters?.let { "accuracy" to it.plain() },
            point.baroAltitudeMeters?.let { "baro" to it.plain() },
        )
        if (extensions.isNotEmpty()) {
            out.append("<extensions>")
            extensions.forEach { (tag, value) -> out.append("<dromos:$tag>").append(value).append("</dromos:$tag>") }
            out.append("</extensions>")
        }
        out.append("</trkpt>\n")
    }

    /** Les noms de Garmin, les plus répandus ; relus par [GpxReader]. */
    private fun typeName(type: ActivityType) = when (type) {
        ActivityType.RUN -> "running"
        ActivityType.WALK -> "walking"
    }
}
