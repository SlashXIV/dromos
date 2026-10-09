package com.gabrielifrim.dromos.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette PROVISOIRE : la direction artistique définitive arrive au jalon J8.
// Pas de couleurs dynamiques (Material You) : la lisibilité en plein soleil
// exige des contrastes maîtrisés, pas ceux du fond d'écran de l'utilisateur.
private val Asphalt = Color(0xFF101214)
private val AsphaltRaised = Color(0xFF1A1D21)
private val AsphaltEdge = Color(0xFF2A2E33)
private val Chalk = Color(0xFFF2F0EA)
private val ChalkDim = Color(0xFFA9AEB4)
private val Paper = Color(0xFFFAFAF7)
private val Ink = Color(0xFF111315)
private val InkDim = Color(0xFF4A4F55)
private val Signal = Color(0xFFFF6A3D)
private val SignalDeep = Color(0xFFB83A12)

private val DarkScheme = darkColorScheme(
    primary = Signal,
    onPrimary = Asphalt,
    background = Asphalt,
    onBackground = Chalk,
    surface = Asphalt,
    onSurface = Chalk,
    surfaceVariant = AsphaltRaised,
    onSurfaceVariant = ChalkDim,
    surfaceContainer = AsphaltRaised,
    outline = AsphaltEdge,
)

private val LightScheme = lightColorScheme(
    primary = SignalDeep,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECECE7),
    onSurfaceVariant = InkDim,
    surfaceContainer = Color(0xFFF1F1EC),
    outline = Color(0xFFD5D5CF),
)

@Composable
fun DromosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
