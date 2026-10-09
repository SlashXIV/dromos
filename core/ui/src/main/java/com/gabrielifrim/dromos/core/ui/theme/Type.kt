package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gabrielifrim.dromos.core.ui.R

/**
 * Trois familles, embarquées (licence OFL, aucune requête réseau) :
 * - Syne : titres « affiche », comme le site de l'auteur ;
 * - Barlow Condensed : chiffres, silhouette des tableaux de chronométrage ;
 * - Manrope : texte courant.
 * Syne et Manrope sont des polices variables : la graisse est fixée par axe.
 */
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Syne = FontFamily(variable(R.font.syne, 800))

val Manrope = FontFamily(
    variable(R.font.manrope, 400),
    variable(R.font.manrope, 500),
    variable(R.font.manrope, 600),
    variable(R.font.manrope, 700),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Chiffres à chasse fixe : un chrono qui avance ne fait pas danser la ligne. */
private const val TABULAR = "tnum"

private val Tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/** Styles propres à Dromos, nommés par usage (et non par taille). */
@Immutable
data class DromosType(
    /** Titre d'affiche : Syne ExtraBold, interlignage serré, à écrire en capitales. */
    val poster: TextStyle,
    /** Durée en cours de sortie. */
    val metricHuge: TextStyle,
    /** Mesure principale (distance, allure). */
    val metricLarge: TextStyle,
    /** Mesure secondaire (cartes, splits). */
    val metric: TextStyle,
    /** Petit surtitre de section : capitales espacées. */
    val eyebrow: TextStyle,
    /** Télémétrie : monospace, capitales espacées (« GPS · 4 M · 1 HZ »). */
    val telemetry: TextStyle,
    /** Libellé de bouton principal. */
    val action: TextStyle,
)

internal val DefaultDromosType = DromosType(
    poster = TextStyle(fontFamily = Syne, fontWeight = FontWeight(800), fontSize = 52.sp, lineHeight = 0.92.em, letterSpacing = (-0.04).em, lineHeightStyle = Tight),
    metricHuge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 112.sp, lineHeight = 0.9.em, fontFeatureSettings = TABULAR, lineHeightStyle = Tight),
    metricLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 0.95.em, fontFeatureSettings = TABULAR, lineHeightStyle = Tight),
    metric = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 1.em, fontFeatureSettings = TABULAR),
    eyebrow = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.18.em),
    telemetry = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em),
    action = TextStyle(fontFamily = Syne, fontWeight = FontWeight(800), fontSize = 16.sp, letterSpacing = 0.02.em),
)

internal val LocalDromosType = staticCompositionLocalOf { DefaultDromosType }

/** Typographie Material : Manrope partout, Syne pour les grands titres. */
internal val DromosMaterialTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Syne, fontWeight = FontWeight(800)),
        displayMedium = displayMedium.copy(fontFamily = Syne, fontWeight = FontWeight(800)),
        displaySmall = displaySmall.copy(fontFamily = Syne, fontWeight = FontWeight(800)),
        headlineLarge = headlineLarge.copy(fontFamily = Syne, fontWeight = FontWeight(800)),
        headlineMedium = headlineMedium.copy(fontFamily = Syne, fontWeight = FontWeight(800)),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        bodySmall = bodySmall.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    )
}
