package com.gabrielifrim.dromos.core.ui.visual

import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Courbes de niveau vivantes : le motif signature de Dromos (cousin du fond
 * du site de l'auteur). Un relief de bosses qui dérivent lentement, tracé en
 * isolignes (marching squares) ; le doigt soulève le terrain, l'inclinaison
 * du téléphone le fait glisser.
 *
 * Coût maîtrisé : 24 images/s au plus, en pause dès que l'écran n'est plus au
 * premier plan, image figée en ambiance sobre ou sans animations.
 */
@Composable
fun ContourField(
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    levels: Int = 7,
) {
    val level = LocalMotionLevel.current
    var time by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableFloatStateOf(1f) }
    // Doigt : position cible et force, rejointes en douceur image après image.
    var tipX by remember { mutableFloatStateOf(0.5f) }
    var tipY by remember { mutableFloatStateOf(0.5f) }
    var tipTargetX by remember { mutableFloatStateOf(0.5f) }
    var tipTargetY by remember { mutableFloatStateOf(0.5f) }
    var tipStrength by remember { mutableFloatStateOf(0f) }
    var tipTarget by remember { mutableFloatStateOf(0f) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    LifecycleResumeEffect(Unit) {
        running = 1f
        onPauseOrDispose { running = 0f }
    }

    if (level.decorative) {
        // Mesure brute de l'accéléromètre, hors état : lue au rythme des images. Écrite
        // dans l'état à chaque événement, elle redessinait le relief en plus des 24 i/s.
        val tilt = remember { FloatArray(2) }
        LaunchedEffect(running) {
            if (running == 0f) return@LaunchedEffect
            var last = 0L
            while (true) {
                // Attendre entre deux images plutôt que se réveiller à chaque rafraîchissement
                // de l'écran (60 à 120 Hz) pour n'avancer qu'une fois sur trois ou cinq.
                delay(FRAME_MS)
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else (now - last) / 1e9f
                    last = now
                    time += dt
                    tipX += (tipTargetX - tipX) * 0.12f
                    tipY += (tipTargetY - tipY) * 0.12f
                    tipStrength += (tipTarget - tipStrength) * 0.08f
                    tiltX += (tilt[0] - tiltX) * 0.1f
                    tiltY += (tilt[1] - tiltY) * 0.1f
                }
            }
        }
        TiltEffect { x, y -> tilt[0] = x; tilt[1] = y }
    }

    Canvas(
        modifier.pointerInput(level) {
            if (!level.decorative) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                tipTargetX = down.position.x / size.width
                tipTargetY = down.position.y / size.height
                tipTarget = 1f
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let {
                        tipTargetX = it.position.x / size.width
                        tipTargetY = it.position.y / size.height
                    }
                } while (event.changes.any { it.pressed })
                tipTarget = 0f
            }
        },
    ) {
        drawContours(time, tipX, tipY, tipStrength, tiltX, tiltY, ink, accent, levels)
    }
}

/** 24 images/s : le relief dérive lentement, l'œil ne voit pas la différence avec 60. */
private const val FRAME_MS = 41L

private data class Bump(val u: Float, val v: Float, val amp: Float, val radius: Float, val su: Float, val sv: Float, val phase: Float)

private val BUMPS = listOf(
    Bump(0.22f, 0.42f, 0.9f, 0.26f, 0.11f, 0.13f, 0f),
    Bump(0.74f, 0.24f, 0.7f, 0.21f, 0.09f, 0.07f, 1.7f),
    Bump(0.58f, 0.86f, 0.8f, 0.27f, 0.07f, 0.1f, 3.1f),
    Bump(0.92f, 0.7f, 0.45f, 0.16f, 0.12f, 0.08f, 4.4f),
)

private fun DrawScope.drawContours(
    t: Float, tipX: Float, tipY: Float, tipStrength: Float, tiltX: Float, tiltY: Float,
    ink: Color, accent: Color, levels: Int,
) {
    // Maille de 18 dp : assez fine pour des courbes lisses, assez grosse pour rester légère.
    val cell = 18.dp.toPx()
    val cols = (size.width / cell).toInt() + 2
    val rows = (size.height / cell).toInt() + 2
    val aspect = size.width / size.height
    // Position des bosses : elle ne dépend que du temps, calculée une fois par image.
    val bumpU = FloatArray(BUMPS.size) { BUMPS[it].u + sin(t * BUMPS[it].su + BUMPS[it].phase) * 0.12f }
    val bumpV = FloatArray(BUMPS.size) { BUMPS[it].v + cos(t * BUMPS[it].sv + BUMPS[it].phase) * 0.1f }
    val field = Array(rows) { j ->
        FloatArray(cols) { i ->
            val u = i * cell / size.width + tiltX * 0.05f
            val v = j * cell / size.height + tiltY * 0.05f
            var z = 0f
            for (k in BUMPS.indices) {
                val b = BUMPS[k]
                val du = (u - bumpU[k]) * aspect
                val dv = v - bumpV[k]
                z += b.amp * exp(-(du * du + dv * dv) / (b.radius * b.radius))
            }
            if (tipStrength > 0.001f) {
                val du = (u - tipX) * aspect
                val dv = v - tipY
                z += tipStrength * 0.85f * exp(-(du * du + dv * dv) / 0.018f)
            }
            z + 0.05f * sin(u * 9f + t * 0.2f) * cos(v * 7f - t * 0.15f)
        }
    }
    val inkPaint = strokePaint(ink.copy(alpha = 0.14f), 1.dp.toPx())
    val accentPaint = strokePaint(accent.copy(alpha = 0.55f), 1.3.dp.toPx())
    // Segments à plat (x0, y0, x1, y1…) tracés d'un seul appel : en Path de milliers de
    // petits morceaux, le relief était retriangulé à chaque image par le moteur de rendu.
    val lines = FloatArray((rows - 1) * (cols - 1) * 8)
    val px = FloatArray(4)
    val py = FloatArray(4)
    drawIntoCanvas { canvas ->
        for (k in 0 until levels) {
            val threshold = 0.15f + k * 0.11f
            var count = 0
            for (j in 0 until rows - 1) for (i in 0 until cols - 1) {
                val a = field[j][i]; val b = field[j][i + 1]; val c = field[j + 1][i + 1]; val d = field[j + 1][i]
                val x0 = i * cell; val y0 = j * cell
                var n = crossing(a, b, threshold, x0, y0, x0 + cell, y0, px, py, 0)
                n = crossing(b, c, threshold, x0 + cell, y0, x0 + cell, y0 + cell, px, py, n)
                n = crossing(c, d, threshold, x0 + cell, y0 + cell, x0, y0 + cell, px, py, n)
                n = crossing(d, a, threshold, x0, y0 + cell, x0, y0, px, py, n)
                var s = 0
                while (s + 1 < n) {
                    lines[count++] = px[s]; lines[count++] = py[s]
                    lines[count++] = px[s + 1]; lines[count++] = py[s + 1]
                    s += 2
                }
            }
            // Une ligne sur quatre en couleur d'accent, comme une courbe maîtresse.
            canvas.nativeCanvas.drawLines(lines, 0, count, if (k % 4 == 3) accentPaint else inkPaint)
        }
    }
}

/** Point où l'isoligne [threshold] coupe l'arête (a → b) de la case, s'il existe. */
private fun crossing(
    vp: Float, vq: Float, threshold: Float, ax: Float, ay: Float, bx: Float, by: Float,
    px: FloatArray, py: FloatArray, n: Int,
): Int {
    if ((vp < threshold) == (vq < threshold)) return n
    val f = (threshold - vp) / (vq - vp)
    px[n] = ax + (bx - ax) * f
    py[n] = ay + (by - ay) * f
    return n + 1
}

private fun strokePaint(color: Color, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = width
    this.color = color.toArgb()
}

/** Inclinaison du téléphone (accéléromètre), seulement pendant que l'écran est affiché. */
@Composable
private fun TiltEffect(onTilt: (Float, Float) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onTilt(-event.values[0] / SensorManager.GRAVITY_EARTH, event.values[1] / SensorManager.GRAVITY_EARTH)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager?.unregisterListener(listener) }
    }
}
