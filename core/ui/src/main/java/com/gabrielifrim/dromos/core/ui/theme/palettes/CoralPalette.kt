package com.gabrielifrim.dromos.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.gabrielifrim.dromos.core.ui.R
import com.gabrielifrim.dromos.core.ui.theme.Accents
import com.gabrielifrim.dromos.core.ui.theme.DromosPalette
import com.gabrielifrim.dromos.core.ui.theme.MetricColors
import com.gabrielifrim.dromos.core.ui.theme.PaletteVariant
import com.gabrielifrim.dromos.core.ui.theme.darkScheme
import com.gabrielifrim.dromos.core.ui.theme.lightScheme

private val VibrantCoral = Color(0xFFEE6055)
private val Emerald = Color(0xFF60D394)
private val LightGreen = Color(0xFFAAF683)
private val Jasmine = Color(0xFFFFD97D)
private val Salmon = Color(0xFFFF9B85)

// Sur fond clair, les teintes d'origine sont trop pâles pour du texte (corail ≈ 3:1) :
// on les fonce jusqu'à ≥ 4,5:1 en gardant la teinte. Les originales restent en conteneurs.
private val CoralDeep = Color(0xFFB83A2E)
private val EmeraldDeep = Color(0xFF23804D)
private val JasmineDeep = Color(0xFF8A6200)

private val Ink = Color(0xFF111315)

// « Plus rapide / plus lent » : une paire froide / chaude, distincte pour les
// daltoniens (deutéranopie comprise), vérifiée par PaletteContrastTest.
private val FasterBlue = Color(0xFF3D8FD6)
private val SlowerOrange = Color(0xFFE8693F)
private val FasterBlueDeep = Color(0xFF1F6FB2)
private val SlowerOrangeDeep = Color(0xFFB8542A)

/** Palette « Corail » : énergie chaude et vert de renouveau. Palette par défaut. */
internal val CoralPalette = DromosPalette(
    id = "corail",
    nameRes = R.string.palette_coral,
    swatches = listOf(VibrantCoral, Emerald, LightGreen, Jasmine, Salmon),
    dark = PaletteVariant(
        scheme = darkScheme(
            Accents(
                primary = VibrantCoral, onPrimary = Ink,
                primaryContainer = Color(0xFF4A1F1A), onPrimaryContainer = Salmon,
                secondary = Emerald, onSecondary = Ink,
                secondaryContainer = Color(0xFF173A27), onSecondaryContainer = LightGreen,
                tertiary = Jasmine, onTertiary = Ink,
                tertiaryContainer = Color(0xFF3D3110), onTertiaryContainer = Jasmine,
            ),
        ),
        metrics = MetricColors(
            trace = VibrantCoral,
            faster = FasterBlue,
            slower = SlowerOrange,
            elevation = Jasmine,
            heartRate = VibrantCoral,
        ),
    ),
    light = PaletteVariant(
        scheme = lightScheme(
            Accents(
                primary = CoralDeep, onPrimary = Color.White,
                primaryContainer = Color(0xFFFFD9D2), onPrimaryContainer = Color(0xFF5A1710),
                secondary = EmeraldDeep, onSecondary = Color.White,
                secondaryContainer = LightGreen, onSecondaryContainer = Color(0xFF173A05),
                tertiary = JasmineDeep, onTertiary = Color.White,
                tertiaryContainer = Jasmine, onTertiaryContainer = Color(0xFF3D2C00),
            ),
        ),
        metrics = MetricColors(
            trace = CoralDeep,
            faster = FasterBlueDeep,
            slower = SlowerOrangeDeep,
            elevation = JasmineDeep,
            heartRate = CoralDeep,
        ),
    ),
)
