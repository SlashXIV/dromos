package com.gabrielifrim.dromos.feature.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.domain.tracking.TrackingEvent
import com.gabrielifrim.dromos.core.ui.component.FitText
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import kotlin.math.roundToInt

/**
 * Jauge d'allure : où se situe l'allure du moment par rapport à la moyenne de la
 * sortie. Plus rapide à gauche, plus lent à droite ; les bords valent ±[GAUGE_RANGE_S] s/km.
 */
@Composable
fun PaceGauge(currentSecondsPerKm: Double?, averageSecondsPerKm: Double?, modifier: Modifier = Modifier) {
    val faster = DromosTheme.metrics.faster
    val slower = DromosTheme.metrics.slower
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val mark = MaterialTheme.colorScheme.onSurfaceVariant
    val known = currentSecondsPerKm != null && averageSecondsPerKm != null
    val target = if (known) {
        (0.5 + (currentSecondsPerKm!! - averageSecondsPerKm!!) / (2 * GAUGE_RANGE_S)).toFloat().coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val position by animateFloatAsState(
        target,
        if (LocalMotionLevel.current.animates) tween(Motion.ENTER_MS, easing = Motion.Enter) else tween(0),
        label = "jauge",
    )
    val knob = when {
        !known -> mark
        position < 0.5f -> faster
        else -> slower
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryLabel(stringResource(R.string.record_gauge_faster), color = mark)
            averageSecondsPerKm?.let {
                TelemetryLabel(stringResource(R.string.record_gauge_average, Format.paceFromSeconds(it)), color = mark)
            }
            TelemetryLabel(stringResource(R.string.record_gauge_slower), color = mark)
        }
        // Hauteur = halo du curseur : rien ne déborde, même sous la désaturation qui rogne au cadre.
        Canvas(Modifier.fillMaxWidth().height(32.dp)) {
            val barHeight = 10.dp.toPx()
            val top = (size.height - barHeight) / 2
            drawRoundRect(
                Brush.horizontalGradient(listOf(faster.copy(alpha = 0.25f), track, slower.copy(alpha = 0.25f))),
                Offset(0f, top), Size(size.width, barHeight), CornerRadius(barHeight / 2),
            )
            drawLine(mark, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2.dp.toPx())
            val radius = 10.dp.toPx()
            val x = radius + (size.width - 2 * radius) * position
            drawCircle(knob.copy(alpha = 0.3f), radius * 1.6f, Offset(x, size.height / 2))
            drawCircle(knob, radius, Offset(x, size.height / 2))
        }
    }
}

/**
 * Bandeau de kilomètre : tombe du haut avec un léger rebond, reste
 * le temps d’être lu (RecordViewModel.BANNER_MS), repart. Lu par TalkBack au passage.
 */
@Composable
fun KmBanner(event: TrackingEvent.Kilometer?, modifier: Modifier = Modifier) {
    val animates = LocalMotionLevel.current.animates
    // Garde le dernier contenu pendant la sortie du bandeau.
    var shown by remember { mutableStateOf(event) }
    if (event != null) shown = event
    AnimatedVisibility(
        visible = event != null,
        modifier = modifier,
        enter = if (animates) slideInVertically(tween(Motion.RELEASE_MS, easing = Motion.Release)) { -it * 2 } + fadeIn() else fadeIn(tween(0)),
        exit = if (animates) slideOutVertically(tween(Motion.TRANSITION_MS, easing = Motion.Transition)) { -it * 2 } + fadeOut() else fadeOut(tween(0)),
    ) {
        val km = shown ?: return@AnimatedVisibility
        val ink = MaterialTheme.colorScheme.onPrimary
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FitText(
                stringResource(R.string.record_km_banner, km.number).uppercase(),
                style = DromosTheme.type.action.copy(fontSize = DromosTheme.type.action.fontSize * 1.15f),
                color = ink,
            )
            km.split?.let { split ->
                FitText(
                    stringResource(R.string.record_km_banner_pace, Format.paceFromSeconds(split.seconds.toDouble())),
                    Modifier.weight(1f),
                    style = DromosTheme.type.metric.copy(fontSize = DromosTheme.type.metric.fontSize * 1.4f),
                    color = ink,
                )
            } ?: Box(Modifier.weight(1f))
            km.versusAverage?.let {
                TelemetryLabel(stringResource(R.string.record_km_banner_delta, Format.paceDelta(it)), color = ink)
            }
        }
    }
}

/** « ● REC » qui clignote en course ; « PAUSE » qui respire à l'arrêt. */
@Composable
fun RecIndicator(paused: Boolean) {
    val decorative = LocalMotionLevel.current.decorative
    val pulse = if (decorative) {
        rememberInfiniteTransition(label = "rec").animateFloat(
            1f, 0.25f,
            infiniteRepeatable(tween(if (paused) BREATH_MS / 2 else BLINK_MS / 2), RepeatMode.Reverse),
            label = "rec",
        ).value
    } else {
        1f
    }
    if (paused) {
        FitText(
            stringResource(R.string.record_paused_label).uppercase(),
            Modifier.graphicsLayer { alpha = pulse },
            style = DromosTheme.type.action.copy(fontSize = DromosTheme.type.action.fontSize * 1.5f),
            color = MaterialTheme.colorScheme.tertiary,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = pulse }
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
            TelemetryLabel(stringResource(R.string.record_rec), color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Contenu passé en niveaux de gris : la sortie est à l'arrêt, les chiffres aussi. */
fun Modifier.desaturate(enabled: Boolean): Modifier = if (!enabled) this else drawWithContent {
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), paint)
        drawContent()
        canvas.restore()
    }
}

/** Points de page : la page courante s'allonge. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                Modifier
                    .width(if (selected) 22.dp else 6.dp)
                    .height(6.dp)
                    .background(ink.copy(alpha = if (selected) 1f else 0.3f), CircleShape),
            )
        }
    }
}

/** Allure en secondes par kilomètre, ou null à l'arrêt (comme [Format.paceFromSpeed]). */
fun secondsPerKm(metersPerSecond: Float?): Double? =
    metersPerSecond?.takeIf { it >= 0.5f }?.let { (1000.0 / it).roundToInt().toDouble() }

private const val GAUGE_RANGE_S = 60.0
private const val BLINK_MS = 1_200
private const val BREATH_MS = 3_200
