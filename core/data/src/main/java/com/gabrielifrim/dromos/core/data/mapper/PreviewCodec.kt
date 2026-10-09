package com.gabrielifrim.dromos.core.data.mapper

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import java.util.Locale

/**
 * Encodage texte compact de l'aperçu : « lat,lon;lat,lon… » à 5 décimales
 * (≈ 1 m, bien assez pour une miniature). Lisible, stable, sans dépendance.
 */
internal object PreviewCodec {

    fun encode(points: List<GeoPoint>): String =
        points.joinToString(";") { String.format(Locale.ROOT, "%.5f,%.5f", it.latitude, it.longitude) }

    /** Une valeur illisible donne null : la miniature sera recalculée. */
    fun decode(value: String?): List<GeoPoint>? {
        if (value.isNullOrBlank()) return if (value == null) null else emptyList()
        return runCatching {
            value.split(';').map { pair ->
                val (lat, lon) = pair.split(',')
                GeoPoint(lat.toDouble(), lon.toDouble())
            }
        }.getOrNull()
    }
}
