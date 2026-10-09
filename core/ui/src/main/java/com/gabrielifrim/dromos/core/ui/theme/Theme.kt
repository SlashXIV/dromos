package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Thème de l'application. Pas de couleurs dynamiques (Material You) : la
 * lisibilité en plein soleil exige des contrastes vérifiés, pas ceux du fond
 * d'écran de l'utilisateur.
 */
@Composable
fun DromosTheme(
    palette: DromosPalette = Palettes.default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val variant = if (darkTheme) palette.dark else palette.light
    CompositionLocalProvider(LocalMetricColors provides variant.metrics) {
        MaterialTheme(colorScheme = variant.scheme, content = content)
    }
}

/** Accès aux couleurs propres à Dromos, en complément de MaterialTheme. */
object DromosTheme {
    val metrics: MetricColors
        @Composable @ReadOnlyComposable
        get() = LocalMetricColors.current
}
