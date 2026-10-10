package com.gabrielifrim.dromos.feature.activity

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.motion.RisingText
import com.gabrielifrim.dromos.core.ui.text.displayTitle
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Balayage « ligne d'arrivée » : une bande en damier traverse l'écran de haut en
 * bas, auréolée d'accent. Décoratif : l'appelant ne le compose qu'en ambiance expressive.
 */
@Composable
fun FinishLineSweep(onEnd: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SWEEP_MS, easing = Motion.Transition))
        onEnd()
    }
    val light = MaterialTheme.colorScheme.surface
    val dark = MaterialTheme.colorScheme.onSurface
    val glow = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val band = 28.dp.toPx()
        val cell = band / 2
        val top = progress.value * (size.height + band) - band
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.45f), Color.Transparent), top - band, top + 2 * band),
            Offset(0f, top - band), Size(size.width, 3 * band),
        )
        var x = 0f
        var column = 0
        while (x < size.width) {
            repeat(2) { row ->
                drawRect(if ((column + row) % 2 == 0) light else dark, Offset(x, top + row * cell), Size(cell, cell))
            }
            x += cell
            column++
        }
    }
}

/** « ● SORTIE ENREGISTRÉE », la date, puis l'affiche « ARRIVÉE. » et le titre de la sortie. */
@Composable
fun FinishHeader(activity: Activity) {
    val locale = LocalConfiguration.current.locales[0]
    val start = activity.startedAt.atZone(ZoneId.systemDefault())
    val date = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", locale).format(start)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TelemetryLabel("● " + stringResource(R.string.activity_saved), Modifier.weight(1f), DromosTheme.metrics.faster)
            TelemetryLabel(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val word = stringResource(R.string.activity_arrival).uppercase(locale)
        RisingText(
            text = "$word.",
            style = DromosTheme.type.poster,
            color = MaterialTheme.colorScheme.onBackground,
            accent = MaterialTheme.colorScheme.primary,
            accentFrom = word.length,
            startDelayMs = SWEEP_MS / 2,
        )
        Text(activity.displayTitle(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Un appui n'importe où, sans le consommer : les boutons restent utilisables. */
fun Modifier.onAnyTouch(enabled: Boolean, onTouch: () -> Unit): Modifier = if (!enabled) this else pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onTouch()
    }
}

const val SWEEP_MS = 840

/** Fin de la séquence d'arrivée : balayage, tracé, totaux et splits. */
const val FINISH_SEQUENCE_MS = 3_200L
