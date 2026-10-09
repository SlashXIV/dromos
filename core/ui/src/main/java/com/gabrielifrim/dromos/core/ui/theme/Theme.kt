package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.motion.MotionLevel

/**
 * Thème de l'application. Pas de couleurs dynamiques (Material You) : la
 * lisibilité en plein soleil exige des contrastes vérifiés, pas ceux du fond
 * d'écran de l'utilisateur.
 *
 * Changer de palette ou de luminosité FOND les couleurs (≈ 0,6 s) au lieu de
 * les basculer d'un coup.
 */
@Composable
fun DromosTheme(
    palette: DromosPalette = Palettes.default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: MotionLevel = MotionLevel.FULL,
    content: @Composable () -> Unit,
) {
    val variant = if (darkTheme) palette.dark else palette.light
    val scheme = animateScheme(variant.scheme, animate = motion.animates)
    CompositionLocalProvider(
        LocalMetricColors provides variant.metrics,
        LocalMotionLevel provides motion,
        LocalDromosType provides DefaultDromosType,
    ) {
        MaterialTheme(colorScheme = scheme, typography = DromosMaterialTypography, content = content)
    }
}

/** Accès aux éléments propres à Dromos, en complément de MaterialTheme. */
object DromosTheme {
    val metrics: MetricColors
        @Composable @ReadOnlyComposable
        get() = LocalMetricColors.current

    val type: DromosType
        @Composable @ReadOnlyComposable
        get() = LocalDromosType.current
}

@Composable
private fun animateScheme(target: ColorScheme, animate: Boolean): ColorScheme {
    @Composable
    fun Color.animated(label: String): Color {
        if (!animate) return this
        val value by animateColorAsState(this, tween(Motion.TRANSITION_MS, easing = Motion.Transition), label = label)
        return value
    }
    return target.copy(
        primary = target.primary.animated("primary"),
        onPrimary = target.onPrimary.animated("onPrimary"),
        primaryContainer = target.primaryContainer.animated("primaryContainer"),
        onPrimaryContainer = target.onPrimaryContainer.animated("onPrimaryContainer"),
        secondary = target.secondary.animated("secondary"),
        onSecondary = target.onSecondary.animated("onSecondary"),
        secondaryContainer = target.secondaryContainer.animated("secondaryContainer"),
        onSecondaryContainer = target.onSecondaryContainer.animated("onSecondaryContainer"),
        tertiary = target.tertiary.animated("tertiary"),
        onTertiary = target.onTertiary.animated("onTertiary"),
        tertiaryContainer = target.tertiaryContainer.animated("tertiaryContainer"),
        onTertiaryContainer = target.onTertiaryContainer.animated("onTertiaryContainer"),
        background = target.background.animated("background"),
        onBackground = target.onBackground.animated("onBackground"),
        surface = target.surface.animated("surface"),
        onSurface = target.onSurface.animated("onSurface"),
        onSurfaceVariant = target.onSurfaceVariant.animated("onSurfaceVariant"),
        surfaceContainer = target.surfaceContainer.animated("surfaceContainer"),
        surfaceContainerLow = target.surfaceContainerLow.animated("surfaceContainerLow"),
        surfaceContainerLowest = target.surfaceContainerLowest.animated("surfaceContainerLowest"),
        surfaceContainerHigh = target.surfaceContainerHigh.animated("surfaceContainerHigh"),
        outline = target.outline.animated("outline"),
        outlineVariant = target.outlineVariant.animated("outlineVariant"),
    )
}
