package com.gabrielifrim.dromos.feature.record

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.motion.rememberHaptics
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import kotlinx.coroutines.delay

/** Rayons des trois couloirs, en fraction du rayon du sonar (planche « Départ »). */
private val LANES = floatArrayOf(0.65f, 0.81f, 0.97f)

/**
 * Recherche GPS en sonar : trois anneaux partent du centre tant que le signal manque,
 * puis se figent en couloirs de piste à l'accroche. Sans mouvement décoratif, les
 * couloirs sont dessinés d'emblée et seule leur couleur dit l'état.
 */
@Composable
fun GpsSonar(readiness: GpsReadiness, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onBackground
    val decorative = LocalMotionLevel.current.decorative
    val ready = readiness == GpsReadiness.READY
    val freeze = remember { Animatable(if (ready) 1f else 0f) }
    LaunchedEffect(ready, decorative) {
        val target = if (ready) 1f else 0f
        if (decorative) freeze.animateTo(target, tween(Motion.TRANSITION_MS, easing = Motion.Enter)) else freeze.snapTo(target)
    }
    val sweep = if (decorative && !ready) {
        rememberInfiniteTransition(label = "sonar")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(SONAR_MS, easing = LinearEasing)), label = "phase").value
    } else {
        0f
    }
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2
        val stroke = Stroke(1.5.dp.toPx())
        LANES.forEach { drawCircle(ink.copy(alpha = 0.08f), it * maxRadius, center, style = stroke) }
        val f = freeze.value
        if (decorative && f < 1f) {
            repeat(3) { i ->
                val phase = (sweep + i / 3f) % 1f
                val eased = Motion.Enter.transform(phase)
                drawCircle(
                    accent.copy(alpha = 0.9f * (1f - phase) * (1f - f)),
                    (0.25f + 0.75f * eased) * maxRadius, center, style = stroke,
                )
            }
        }
        val laneAlpha = if (decorative) f else if (readiness == GpsReadiness.OFF) 0.15f else if (ready) 1f else 0.35f
        LANES.forEach { drawCircle(accent.copy(alpha = 0.8f * laneAlpha), it * maxRadius, center, style = stroke) }
    }
}

/** Halo qui respire autour du bouton DÉMARRER (3,2 s), rien en ambiance sobre. */
@Composable
fun BreathingHalo(buttonSize: Dp, modifier: Modifier = Modifier) {
    if (!LocalMotionLevel.current.decorative) return
    val accent = MaterialTheme.colorScheme.primary
    val breath by rememberInfiniteTransition(label = "souffle").animateFloat(
        0f, 1f, infiniteRepeatable(tween(BREATH_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "souffle",
    )
    Canvas(modifier) {
        val radius = buttonSize.toPx() / 2 + 18.dp.toPx() * breath
        drawCircle(accent.copy(alpha = 0.45f * (1f - breath)), radius)
    }
}

/** Télémétrie GPS de l'écran de départ, point clignotant pendant la recherche. */
@Composable
fun GpsTelemetry(gps: GpsWarmup) {
    val text = when (gps.readiness) {
        GpsReadiness.OFF -> stringResource(R.string.record_gps_off)
        GpsReadiness.SEARCHING -> stringResource(R.string.record_gps_searching)
        GpsReadiness.READY -> gps.accuracyMeters?.let { stringResource(R.string.record_gps_ready_accuracy, it.toInt()) }
            ?: stringResource(R.string.record_gps_ready)
    }
    val color = when (gps.readiness) {
        GpsReadiness.READY -> DromosTheme.metrics.faster
        GpsReadiness.SEARCHING -> MaterialTheme.colorScheme.tertiary
        GpsReadiness.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val blink = if (gps.readiness == GpsReadiness.SEARCHING && LocalMotionLevel.current.decorative) {
        rememberInfiniteTransition(label = "clignote")
            .animateFloat(1f, 0.25f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "clignote").value
    } else {
        1f
    }
    TelemetryLabel("● $text", Modifier.graphicsLayer { alpha = blink }, color)
}

/**
 * Compte à rebours 3-2-1 plein écran, une vibration par seconde. Un appui l'annule.
 * Fonctionnel plus que décoratif : il compte même sans animations, seul le zoom des
 * chiffres disparaît.
 */
@Composable
fun Countdown(onDone: () -> Unit, onCancel: () -> Unit) {
    val haptics = rememberHaptics()
    val animates = LocalMotionLevel.current.animates
    var value by remember { mutableIntStateOf(COUNT_FROM) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        for (n in COUNT_FROM downTo 1) {
            value = n
            haptics.tick()
            if (animates) {
                pop.snapTo(1.35f)
                pop.animateTo(1f, tween(450, easing = Motion.Release))
                delay(550)
            } else {
                delay(1000)
            }
        }
        haptics.confirm()
        onDone()
    }
    val description = stringResource(R.string.record_countdown_a11y, value)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                value.toString(),
                style = DromosTheme.type.metricHuge.copy(fontSize = 280.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = (2f - pop.value).coerceIn(0f, 1f)
                },
            )
            TelemetryLabel(stringResource(R.string.record_countdown_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Balayage « ligne de départ » : un aplat d'accent traverse l'écran de gauche à
 * droite, son bord d'attaque marqué d'un trait clair. Décoratif seulement.
 */
@Composable
fun StartSweep(onEnd: () -> Unit) {
    val decorative = LocalMotionLevel.current.decorative
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (decorative) progress.animateTo(1f, tween(Motion.TRANSITION_MS, easing = Motion.Transition))
        onEnd()
    }
    if (!decorative) return
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.onPrimary
    Canvas(Modifier.fillMaxSize()) {
        // L'aplat entre par la gauche, couvre l'écran à mi-course, sort par la droite.
        val left = (progress.value * 2f - 1f) * size.width
        drawRect(accent, Offset(left, 0f), Size(size.width, size.height))
        drawRect(line, Offset(left + size.width - 4.dp.toPx(), 0f), Size(4.dp.toPx(), size.height))
    }
}

private const val SONAR_MS = 2_400
private const val BREATH_MS = 3_200
private const val COUNT_FROM = 3

