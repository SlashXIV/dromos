package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.pow

/**
 * Garde-fou de lisibilité en extérieur, appliqué à CHAQUE palette du registre :
 * une palette ajoutée qui ne tient pas ces seuils fait échouer le build.
 *
 * Seuils WCAG 2.2 : 7:1 (AAA) pour le texte courant, 4,5:1 (AA) pour le texte
 * sur couleur d'accent, 3:1 pour les éléments graphiques (tracé, courbes).
 */
class PaletteContrastTest {

    private fun variants() = Palettes.all.flatMap { p ->
        listOf("${p.id}/clair" to p.light, "${p.id}/sombre" to p.dark)
    }

    @Test
    fun `le texte courant atteint 7 pour 1 sur le fond`() = variants().forEach { (name, v) ->
        v.scheme.run {
            assertContrast(name, "onBackground/background", onBackground, background, 7.0)
            assertContrast(name, "onSurface/surfaceContainer", onSurface, surfaceContainer, 7.0)
        }
    }

    @Test
    fun `le texte secondaire et les accents atteignent 4,5 pour 1`() = variants().forEach { (name, v) ->
        v.scheme.run {
            assertContrast(name, "onSurfaceVariant/background", onSurfaceVariant, background, 4.5)
            assertContrast(name, "onSurfaceVariant/surfaceContainer", onSurfaceVariant, surfaceContainer, 4.5)
            listOf(
                "primary" to (primary to onPrimary),
                "secondary" to (secondary to onSecondary),
                "tertiary" to (tertiary to onTertiary),
                "primaryContainer" to (primaryContainer to onPrimaryContainer),
                "secondaryContainer" to (secondaryContainer to onSecondaryContainer),
                "tertiaryContainer" to (tertiaryContainer to onTertiaryContainer),
            ).forEach { (role, pair) -> assertContrast(name, "on-$role/$role", pair.second, pair.first, 4.5) }
            // Les accents servent aussi de couleur de texte (liens, valeurs mises en avant).
            assertContrast(name, "primary/background", primary, background, 4.5)
            assertContrast(name, "secondary/background", secondary, background, 4.5)
            assertContrast(name, "tertiary/background", tertiary, background, 4.5)
        }
    }

    @Test
    fun `les couleurs de mesure se détachent du fond`() = variants().forEach { (name, v) ->
        val bg = v.scheme.background
        v.metrics.run {
            listOf("trace" to trace, "faster" to faster, "slower" to slower, "elevation" to elevation, "heartRate" to heartRate)
                .forEach { (role, color) -> assertContrast(name, "$role/background", color, bg, 3.0) }
        }
    }

    @Test
    fun `les identifiants de palette sont uniques`() {
        val ids = Palettes.all.map { it.id }
        assertWithMessage("identifiants en double").that(ids).containsNoDuplicates()
    }

    private fun assertContrast(palette: String, pair: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertWithMessage("$palette : $pair = %.2f:1, minimum $min:1".format(ratio)).that(ratio).isAtLeast(min)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Luminance relative sRGB, définition WCAG. */
    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }
}
