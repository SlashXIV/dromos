package com.gabrielifrim.dromos.core.ui.visual

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import kotlinx.coroutines.delay
import kotlin.math.cos

/**
 * Miniature d'un tracé, qui se dessine du départ à l'arrivée à son apparition.
 * Projection équirectangulaire, même échelle sur les deux axes, nord en haut.
 */
@Composable
fun TraceThumbnail(
    points: List<GeoPoint>,
    color: Color,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
) {
    val level = LocalMotionLevel.current
    val progress = remember(points) { Animatable(if (level.decorative) 0f else 1f) }
    LaunchedEffect(points) {
        if (!level.decorative) return@LaunchedEffect
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(Motion.DRAW_MS, easing = Motion.Transition))
    }
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val pad = 10.dp.toPx()
        val minLat = points.minOf { it.latitude }; val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }; val maxLon = points.maxOf { it.longitude }
        val kx = cos(Math.toRadians((minLat + maxLat) / 2))
        val spanX = ((maxLon - minLon) * kx).coerceAtLeast(1e-5)
        val spanY = (maxLat - minLat).coerceAtLeast(1e-5)
        val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY)
        val ox = (size.width - spanX * scale) / 2
        val oy = (size.height - spanY * scale) / 2
        fun x(p: GeoPoint) = (ox + (p.longitude - minLon) * kx * scale).toFloat()
        fun y(p: GeoPoint) = (size.height - oy - (p.latitude - minLat) * scale).toFloat()
        val full = Path().apply {
            moveTo(x(points.first()), y(points.first()))
            points.drop(1).forEach { lineTo(x(it), y(it)) }
        }
        val path = if (progress.value >= 1f) full else Path().also { partial ->
            val measure = PathMeasure().apply { setPath(full, false) }
            measure.getSegment(0f, measure.length * progress.value, partial, true)
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(x(points.first()), y(points.first())))
    }
}
