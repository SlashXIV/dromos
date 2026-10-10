package com.gabrielifrim.dromos.feature.activity

import androidx.compose.ui.semantics.heading
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.domain.analysis.ProfileSample
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import com.gabrielifrim.dromos.core.ui.R as UiR
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Profils d'altitude et d'allure, l'un sous l'autre (jamais deux échelles sur un même
 * graphique), avec un curseur commun : glisser le doigt sur l'un place le curseur sur
 * les deux et sur la carte. Les chiffres lus s'affichent au-dessus, en clair : la
 * couleur n'est jamais seule à porter une valeur, et les splits restent la vue tabulaire.
 */
@Composable
fun Profiles(
    samples: List<ProfileSample>,
    cursor: Int?,
    onCursor: (Int?) -> Unit,
    averagePace: Double?,
    modifier: Modifier = Modifier,
    delayMs: Long = 0,
) {
    val altitudes = samples.mapNotNull { it.elevationMeters }
    // Une altitude constante (pas de mesure réelle, ou 0 renvoyé par défaut) ne dit rien.
    val hasElevation = altitudes.size >= 2 && altitudes.max() - altitudes.min() >= 1.0
    val paces = samples.mapNotNull { it.paceSecondsPerKm }
    if (samples.size < 2 || (!hasElevation && paces.size < 2)) return
    val progress = drawOn(delayMs)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Readout(samples, cursor, hasElevation)
        if (hasElevation) {
            ChartTitle(stringResource(R.string.activity_profile_elevation))
            ElevationChart(samples, cursor, onCursor, progress)
        }
        if (paces.size >= 2) {
            ChartTitle(stringResource(R.string.activity_profile_pace))
            PaceChart(samples, paces.sorted(), averagePace ?: paces.average(), cursor, onCursor, progress)
        }
    }
}

/** La ligne de lecture : distance, altitude et allure au curseur (ou à l'arrivée sans curseur). */
@Composable
private fun Readout(samples: List<ProfileSample>, cursor: Int?, hasElevation: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val sample = samples[cursor ?: samples.lastIndex]
    val parts = buildList {
        add(stringResource(R.string.activity_profile_km, Format.kilometers(sample.distanceMeters, locale)))
        sample.elevationMeters?.takeIf { hasElevation }?.let { add(stringResource(R.string.activity_profile_altitude, Format.meters(it))) }
        sample.paceSecondsPerKm?.let { add(stringResource(R.string.activity_profile_pace_value, Format.paceFromSeconds(it))) }
    }
    TelemetryLabel(
        parts.joinToString(" · "),
        color = if (cursor == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ChartTitle(text: String) {
    TelemetryLabel(text, Modifier.semantics { heading() }, MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ElevationChart(samples: List<ProfileSample>, cursor: Int?, onCursor: (Int?) -> Unit, progress: Float) {
    val values = samples.mapNotNull { it.elevationMeters }
    val low = values.min()
    // Au moins 20 m d'amplitude : un parcours plat ne doit pas ressembler à une montagne.
    val high = maxOf(values.max(), low + 20.0)
    val color = DromosTheme.metrics.elevation
    val ring = MaterialTheme.colorScheme.surface
    val description = stringResource(R.string.activity_profile_elevation_a11y, Format.meters(low), Format.meters(values.max()))
    ChartFrame(
        samples, cursor, onCursor, description,
        top = stringResource(UiR.string.unit_meters, Format.meters(high)),
        bottom = stringResource(UiR.string.unit_meters, Format.meters(low)),
    ) { x ->
        val y = { v: Double -> (size.height * (1 - (v - low) / (high - low))).toFloat() }
        val line = curve(samples, x, { it.elevationMeters }, y)
        val area = Path().apply {
            addPath(line)
            val last = samples.indexOfLast { it.elevationMeters != null }
            val first = samples.indexOfFirst { it.elevationMeters != null }
            lineTo(x(last), size.height)
            lineTo(x(first), size.height)
            close()
        }
        clipRect(right = size.width * progress) {
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f))))
            drawPath(line, color, style = line())
        }
        cursor?.let { i -> samples[i].elevationMeters?.let { drawCursorDot(Offset(x(i), y(it)), color, ring) } }
    }
}

@Composable
private fun PaceChart(samples: List<ProfileSample>, sorted: List<Double>, average: Double, cursor: Int?, onCursor: (Int?) -> Unit, progress: Float) {
    // Échelle sur les 5e-95e centiles : un arrêt de feu rouge n'écrase pas tout le graphique.
    val fast = sorted[(sorted.size * 0.05).toInt()]
    val slow = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)]
    val pad = maxOf((slow - fast) * 0.15, 10.0)
    val top = fast - pad
    val bottom = slow + pad
    val faster = DromosTheme.metrics.faster
    val slower = DromosTheme.metrics.slower
    val ink = MaterialTheme.colorScheme.onSurface
    val rule = MaterialTheme.colorScheme.onSurfaceVariant
    val ring = MaterialTheme.colorScheme.surface
    val description = stringResource(R.string.activity_profile_pace_a11y, Format.paceFromSeconds(fast), Format.paceFromSeconds(slow))
    ChartFrame(
        samples, cursor, onCursor, description,
        top = stringResource(R.string.activity_profile_faster),
        bottom = stringResource(R.string.activity_profile_slower),
        middle = stringResource(R.string.activity_profile_average, Format.paceFromSeconds(average)),
        middleFraction = ((average - top) / (bottom - top)).toFloat(),
    ) { x ->
        // Plus rapide en haut : on lit « monter » comme « mieux ».
        val y = { v: Double -> (size.height * ((v.coerceIn(top, bottom) - top) / (bottom - top))).toFloat() }
        val mid = y(average)
        val line = curve(samples, x, { it.paceSecondsPerKm }, y)
        val area = Path().apply {
            addPath(line)
            val last = samples.indexOfLast { it.paceSecondsPerKm != null }
            val first = samples.indexOfFirst { it.paceSecondsPerKm != null }
            lineTo(x(last), mid)
            lineTo(x(first), mid)
            close()
        }
        clipRect(right = size.width * progress) {
            // La position dit plus rapide / plus lent ; la teinte ne fait que le rappeler.
            clipRect(bottom = mid) { drawPath(area, faster.copy(alpha = 0.3f)) }
            clipRect(top = mid) { drawPath(area, slower.copy(alpha = 0.3f)) }
            drawLine(rule, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 1.dp.toPx())
            drawPath(line, ink, style = line())
        }
        cursor?.let { i -> samples[i].paceSecondsPerKm?.let { drawCursorDot(Offset(x(i), y(it)), ink, ring) } }
    }
}

/**
 * Cadre commun : bornes en texte à gauche, distance en bas, curseur au doigt (appui ou
 * glissé ; la cible est toute la hauteur du graphique, pas la courbe).
 */
@Composable
private fun ChartFrame(
    samples: List<ProfileSample>,
    cursor: Int?,
    onCursor: (Int?) -> Unit,
    description: String,
    top: String,
    bottom: String,
    middle: String? = null,
    middleFraction: Float = 0.5f,
    draw: DrawScope.(x: (Int) -> Float) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val total = samples.last().distanceMeters.coerceAtLeast(1.0)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val cursorColor = MaterialTheme.colorScheme.onSurfaceVariant
    fun indexAt(px: Float, width: Float): Int {
        val target = (px / width).coerceIn(0f, 1f) * total
        return samples.indices.minBy { abs(samples[it].distanceMeters - target) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
                    .semantics { contentDescription = description }
                    .pointerInput(samples) {
                        detectTapGestures { onCursor(indexAt(it.x, size.width.toFloat())) }
                    }
                    .pointerInput(samples) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            onCursor(indexAt(change.position.x, size.width.toFloat()))
                        }
                    },
            ) {
                val x = { i: Int -> (size.width * samples[i].distanceMeters / total).toFloat() }
                drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                draw(x)
                cursor?.let { drawLine(cursorColor, Offset(x(it), 0f), Offset(x(it), size.height), strokeWidth = 1.dp.toPx()) }
            }
            TelemetryLabel(top, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (middle != null) {
                // Étiquette de la moyenne, posée juste au-dessus de sa ligne, à droite.
                Box(Modifier.fillMaxWidth().height(CHART_HEIGHT * middleFraction.coerceIn(0.15f, 0.85f))) {
                    // Fond de surface : l'étiquette reste lisible là où la courbe la croise.
                    TelemetryLabel(
                        middle,
                        Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TelemetryLabel(bottom, Modifier.align(Alignment.BottomStart).padding(bottom = 4.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryLabel(stringResource(R.string.activity_profile_km, "0"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TelemetryLabel(
                stringResource(R.string.activity_profile_km, Format.kilometers(total, locale)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** La courbe, coupée là où la mesure manque (pause, arrêt) plutôt que reliée à travers. */
private fun curve(samples: List<ProfileSample>, x: (Int) -> Float, value: (ProfileSample) -> Double?, y: (Double) -> Float): Path =
    Path().apply {
        var drawing = false
        samples.forEachIndexed { i, s ->
            val v = value(s)
            if (v == null) {
                drawing = false
            } else if (!drawing) {
                moveTo(x(i), y(v)); drawing = true
            } else {
                lineTo(x(i), y(v))
            }
        }
    }

/** Point du curseur : 8 dp, cerclé de la surface pour rester lisible sur la courbe. */
private fun DrawScope.drawCursorDot(center: Offset, color: Color, ring: Color) {
    drawCircle(ring, 6.dp.toPx(), center)
    drawCircle(color, 4.dp.toPx(), center)
}

/** Trait de courbe : 2 dp, bouts et angles arrondis. */
private fun DrawScope.line() = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Les courbes se tracent de gauche à droite à leur apparition, en ambiance expressive. */
@Composable
private fun drawOn(delayMs: Long): Float {
    val decorative = LocalMotionLevel.current.decorative
    val progress = remember { Animatable(if (decorative) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!decorative) return@LaunchedEffect
        delay(delayMs)
        progress.animateTo(1f, tween(Motion.DRAW_MS, easing = Motion.Transition))
    }
    return if (decorative) progress.value else 1f
}

private val CHART_HEIGHT = 132.dp
