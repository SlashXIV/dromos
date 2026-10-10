package com.gabrielifrim.dromos.core.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel

/**
 * Tracé seul, sans fond de carte : utilisé hors-ligne, quand le fond de carte
 * est désactivé, ou si le style ne peut pas être chargé. Aucune requête réseau.
 */
@Composable
fun TraceCanvas(
    segments: List<List<GeoPoint>>,
    traceColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    pulse: Boolean = false,
) {
    val background = MaterialTheme.colorScheme.surfaceContainer
    // Tête qui pulse (1,6 s) sur le tracé en direct, en ambiance expressive seulement.
    val beat = if (pulse && LocalMotionLevel.current.decorative) {
        rememberInfiniteTransition(label = "tête").animateFloat(
            0f, 1f, infiniteRepeatable(tween(PULSE_MS / 2), RepeatMode.Reverse), label = "tête",
        ).value
    } else {
        0f
    }
    val casing = MaterialTheme.colorScheme.surface
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            val points = segments.flatten()
            if (points.isEmpty()) return@Canvas
            val projection = TraceProjection(segments, size.width, size.height, padding = 24.dp.toPx())
            segments.filter { it.size >= 2 }.forEach { segment ->
                val path = Path().apply {
                    moveTo(projection.x(segment.first()), projection.y(segment.first()))
                    segment.drop(1).forEach { lineTo(projection.x(it), projection.y(it)) }
                }
                drawPath(path, casing, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, traceColor, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val last = points.last()
            val center = Offset(projection.x(last), projection.y(last))
            drawCircle(traceColor.copy(alpha = 0.35f * (1f - beat)), radius = 9.dp.toPx() * (1f + beat), center = center)
            drawCircle(casing, radius = 9.dp.toPx(), center = center)
            drawCircle(traceColor, radius = 6.dp.toPx(), center = center)
        }
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
    }
}

private const val PULSE_MS = 1_600
