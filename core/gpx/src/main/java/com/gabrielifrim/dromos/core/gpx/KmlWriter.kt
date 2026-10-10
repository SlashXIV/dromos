package com.gabrielifrim.dromos.core.gpx

import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.TrackPoint

/**
 * KML 2.2 d'une sortie, pour Google Earth et les cartes en ligne : le tracé seul,
 * une ligne par segment. Export uniquement — sans horodatage par point, un KML ne
 * suffit pas à reconstituer une sortie ; l'aller-retour passe par GPX.
 */
object KmlWriter {

    fun write(activity: Activity, name: String, points: List<TrackPoint>, out: Appendable) {
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        out.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
        out.append("  <name>").append(name.escapeXml()).append("</name>\n")
        // Corail #EE6055, en aabbggrr comme le veut KML.
        out.append("  <Style id=\"trace\"><LineStyle><color>ff5560ee</color><width>4</width></LineStyle></Style>\n")
        out.append("  <Placemark>\n    <name>").append(name.escapeXml()).append("</name>\n")
        activity.notes?.let { out.append("    <description>").append(it.escapeXml()).append("</description>\n") }
        if (points.isNotEmpty()) {
            out.append("    <TimeSpan><begin>").append(points.first().timestamp.toString())
                .append("</begin><end>").append(points.last().timestamp.toString()).append("</end></TimeSpan>\n")
        }
        out.append("    <styleUrl>#trace</styleUrl>\n    <MultiGeometry>\n")
        points.groupBy { it.segment }.toSortedMap().values.forEach { segment ->
            out.append("      <LineString><tessellate>1</tessellate><coordinates>")
            segment.forEachIndexed { index, point ->
                if (index > 0) out.append(' ')
                out.append(point.longitude.plain()).append(',').append(point.latitude.plain())
                point.altitudeMeters?.let { out.append(',').append(it.plain()) }
            }
            out.append("</coordinates></LineString>\n")
        }
        out.append("    </MultiGeometry>\n  </Placemark>\n</Document>\n</kml>\n")
    }
}
