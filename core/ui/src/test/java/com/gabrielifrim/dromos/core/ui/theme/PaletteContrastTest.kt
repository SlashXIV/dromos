package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

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

    /**
     * « Plus rapide / plus lent » doivent rester distincts pour les daltoniens :
     * protanopie et deutéranopie simulées (Machado 2009, sévérité 1), écart OKLab
     * × 100 d'au moins 8 — le seuil du validateur de palettes de la DA.
     */
    @Test
    fun `plus rapide et plus lent restent distincts pour les daltoniens`() = variants().forEach { (name, v) ->
        listOf("protan" to PROTAN, "deutan" to DEUTAN).forEach { (kind, matrix) ->
            val delta = deltaE(simulate(v.metrics.faster, matrix), simulate(v.metrics.slower, matrix))
            assertWithMessage("$name : faster/slower en $kind, ΔE = %.1f, minimum 8".format(delta)).that(delta).isAtLeast(8.0)
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

    private fun simulate(c: Color, m: Array<DoubleArray>): DoubleArray {
        val rgb = doubleArrayOf(srgbToLinear(c.red), srgbToLinear(c.green), srgbToLinear(c.blue))
        return DoubleArray(3) { i -> (m[i][0] * rgb[0] + m[i][1] * rgb[1] + m[i][2] * rgb[2]).coerceIn(0.0, 1.0) }
    }

    private fun deltaE(a: DoubleArray, b: DoubleArray): Double {
        val (la, lb) = oklab(a) to oklab(b)
        return 100 * sqrt((0..2).sumOf { (la[it] - lb[it]).pow(2) })
    }

    private fun oklab(rgb: DoubleArray): DoubleArray {
        val (r, g, b) = Triple(rgb[0], rgb[1], rgb[2])
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun srgbToLinear(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    /** Luminance relative sRGB, définition WCAG. */
    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }

    private companion object {
        val PROTAN = arrayOf(
            doubleArrayOf(0.152286, 1.052583, -0.204868),
            doubleArrayOf(0.114503, 0.786281, 0.099216),
            doubleArrayOf(-0.003882, -0.048116, 1.051998),
        )
        val DEUTAN = arrayOf(
            doubleArrayOf(0.367322, 0.860646, -0.227968),
            doubleArrayOf(0.280085, 0.672501, 0.047413),
            doubleArrayOf(-0.011820, 0.042940, 0.968881),
        )
    }
}
