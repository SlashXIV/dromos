package com.gabrielifrim.dromos.core.ui.visual

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import kotlinx.coroutines.delay

/** Filet fin qui se trace de gauche à droite à son apparition. */
@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outlineVariant, delayMs: Int = 0) {
    val level = LocalMotionLevel.current
    val progress = remember { Animatable(if (level.decorative) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!level.decorative) return@LaunchedEffect
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(550, easing = Motion.Enter))
    }
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(color, Offset.Zero, Offset(size.width * progress.value, 0f), strokeWidth = 1.dp.toPx())
    }
}

/** Petite étiquette de télémétrie : monospace, capitales, couleur d'accent par défaut. */
@Composable
fun TelemetryLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = DromosTheme.type.telemetry, color = color, modifier = modifier)
}

/** Halo d'accent en haut d'écran (8 %), comme le site de l'auteur. */
fun accentHalo(accent: Color): Brush =
    Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.08f), Color.Transparent),
        center = Offset(0.5f, 0f),
        radius = Float.POSITIVE_INFINITY,
    )
