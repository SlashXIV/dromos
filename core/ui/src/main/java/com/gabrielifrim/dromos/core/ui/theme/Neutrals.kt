package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Accents d'une palette pour UNE luminosité. Les neutres (fonds, textes,
 * contours) sont communs à toutes les palettes : ce sont eux qui garantissent
 * la lisibilité en plein soleil, une palette ne fait que colorer.
 */
internal data class Accents(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
)

/** Sombre : noir légèrement bleuté, texte crème. Contraste texte/fond ≈ 16:1. */
internal fun darkScheme(a: Accents): ColorScheme = darkColorScheme(
    primary = a.primary,
    onPrimary = a.onPrimary,
    primaryContainer = a.primaryContainer,
    onPrimaryContainer = a.onPrimaryContainer,
    secondary = a.secondary,
    onSecondary = a.onSecondary,
    secondaryContainer = a.secondaryContainer,
    onSecondaryContainer = a.onSecondaryContainer,
    tertiary = a.tertiary,
    onTertiary = a.onTertiary,
    tertiaryContainer = a.tertiaryContainer,
    onTertiaryContainer = a.onTertiaryContainer,
    background = Color(0xFF111315),
    onBackground = Color(0xFFF4F1EC),
    surface = Color(0xFF111315),
    onSurface = Color(0xFFF4F1EC),
    surfaceVariant = Color(0xFF2A2E33),
    onSurfaceVariant = Color(0xFFA7ABB1),
    surfaceContainerLowest = Color(0xFF0C0E10),
    surfaceContainerLow = Color(0xFF16191C),
    surfaceContainer = Color(0xFF1A1D20),
    surfaceContainerHigh = Color(0xFF22262A),
    surfaceContainerHighest = Color(0xFF2A2E33),
    outline = Color(0xFF6B7077),
    outlineVariant = Color(0xFF2E3237),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF111315),
)

/** Clair : blanc cassé chaud, texte presque noir. Contraste texte/fond ≈ 17:1. */
internal fun lightScheme(a: Accents): ColorScheme = lightColorScheme(
    primary = a.primary,
    onPrimary = a.onPrimary,
    primaryContainer = a.primaryContainer,
    onPrimaryContainer = a.onPrimaryContainer,
    secondary = a.secondary,
    onSecondary = a.onSecondary,
    secondaryContainer = a.secondaryContainer,
    onSecondaryContainer = a.onSecondaryContainer,
    tertiary = a.tertiary,
    onTertiary = a.onTertiary,
    tertiaryContainer = a.tertiaryContainer,
    onTertiaryContainer = a.onTertiaryContainer,
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF14161A),
    surface = Color(0xFFFBFAF7),
    onSurface = Color(0xFF14161A),
    surfaceVariant = Color(0xFFE3E2DD),
    onSurfaceVariant = Color(0xFF4F545B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F4F0),
    surfaceContainer = Color(0xFFEFEEEA),
    surfaceContainerHigh = Color(0xFFE9E8E3),
    surfaceContainerHighest = Color(0xFFE3E2DD),
    outline = Color(0xFF7A7F86),
    outlineVariant = Color(0xFFD6D5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)
