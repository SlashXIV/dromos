package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Couleurs porteuses de sens pour les mesures, hors du ColorScheme de Material :
 * « plus rapide » doit rester lisible comme tel même si une future palette met du
 * vert en couleur primaire. Chaque palette les déclare explicitement.
 */
@Immutable
data class MetricColors(
    /** Tracé GPS sur la carte et dans les miniatures. */
    val trace: Color,
    /** Split plus rapide que la moyenne. */
    val faster: Color,
    /** Split plus lent que la moyenne. */
    val slower: Color,
    /** Profil d'élévation, D+ / D−. */
    val elevation: Color,
    /** Fréquence cardiaque. */
    val heartRate: Color,
)

internal val LocalMetricColors = staticCompositionLocalOf<MetricColors> {
    error("MetricColors absent : envelopper l'écran dans DromosTheme")
}
