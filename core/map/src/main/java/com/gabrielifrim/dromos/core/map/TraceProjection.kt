package com.gabrielifrim.dromos.core.map

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import kotlin.math.cos

/**
 * Projection d'un tracé dans un rectangle de dessin, sans fond de carte :
 * équirectangulaire autour du centre (exacte à l'échelle d'une sortie), même
 * échelle sur les deux axes, tracé centré.
 */
internal class TraceProjection(
    segments: List<List<GeoPoint>>,
    private val width: Float,
    private val height: Float,
    private val padding: Float,
) {
    private val minLat: Double
    private val maxLat: Double
    private val minLon: Double
    private val maxLon: Double
    private val lonScale: Double
    private val scale: Double
    private val offsetX: Double
    private val offsetY: Double

    init {
        val all = segments.flatten()
        minLat = all.minOf { it.latitude }
        maxLat = all.maxOf { it.latitude }
        minLon = all.minOf { it.longitude }
        maxLon = all.maxOf { it.longitude }
        lonScale = cos(Math.toRadians((minLat + maxLat) / 2))
        val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(MIN_SPAN)
        val spanY = (maxLat - minLat).coerceAtLeast(MIN_SPAN)
        val availableW = (width - 2 * padding).coerceAtLeast(1f)
        val availableH = (height - 2 * padding).coerceAtLeast(1f)
        scale = minOf(availableW / spanX, availableH / spanY)
        offsetX = padding + (availableW - ((maxLon - minLon) * lonScale) * scale) / 2
        offsetY = padding + (availableH - (maxLat - minLat) * scale) / 2
    }

    fun x(p: GeoPoint): Float = (offsetX + (p.longitude - minLon) * lonScale * scale).toFloat()

    /** Nord en haut : l'axe y de l'écran descend. */
    fun y(p: GeoPoint): Float = (height - offsetY - (p.latitude - minLat) * scale).toFloat()

    private companion object {
        /** ≈ 10 m : un tracé immobile ne se dilate pas à l'infini. */
        const val MIN_SPAN = 0.0001
    }
}
