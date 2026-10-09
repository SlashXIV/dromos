package com.gabrielifrim.dromos.core.map

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
) {
    val background = MaterialTheme.colorScheme.surfaceContainer
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
