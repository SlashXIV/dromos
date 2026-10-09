package com.gabrielifrim.dromos.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.gabrielifrim.dromos.core.ui.R
import com.gabrielifrim.dromos.core.ui.theme.Accents
import com.gabrielifrim.dromos.core.ui.theme.DromosPalette
import com.gabrielifrim.dromos.core.ui.theme.MetricColors
import com.gabrielifrim.dromos.core.ui.theme.PaletteVariant
import com.gabrielifrim.dromos.core.ui.theme.darkScheme
import com.gabrielifrim.dromos.core.ui.theme.lightScheme

private val ElectricLime = Color(0xFFD4FF3A)
private val Cyan = Color(0xFF3DE1FF)
private val Pink = Color(0xFFFF4D8D)
private val Amber = Color(0xFFFFB238)
private val Ink = Color(0xFF0B0D10)

// Déclinaison claire : mêmes teintes, foncées jusqu'à un contraste de texte ≥ 4,5:1.
private val LimeDeep = Color(0xFF4A5E00)
private val CyanDeep = Color(0xFF006677)
private val PinkDeep = Color(0xFFB0164F)
private val AmberDeep = Color(0xFF855400)

/**
 * Palette « Nocturne » : pensée pour la course de nuit et les écrans OLED.
 * Citron électrique sur encre : le maximum de lisibilité d'un coup d'œil.
 */
internal val NocturnePalette = DromosPalette(
    id = "nocturne",
    nameRes = R.string.palette_nocturne,
    swatches = listOf(Ink, ElectricLime, Cyan, Pink, Amber),
    dark = PaletteVariant(
        scheme = darkScheme(
            Accents(
                primary = ElectricLime, onPrimary = Ink,
                primaryContainer = Color(0xFF2B3310), onPrimaryContainer = ElectricLime,
                secondary = Cyan, onSecondary = Ink,
                secondaryContainer = Color(0xFF0E3540), onSecondaryContainer = Color(0xFFA6F1FF),
                tertiary = Amber, onTertiary = Ink,
                tertiaryContainer = Color(0xFF3F2C08), onTertiaryContainer = Amber,
            ),
        ),
        metrics = MetricColors(
            trace = ElectricLime,
            faster = ElectricLime,
            slower = Pink,
            elevation = Amber,
            heartRate = Pink,
        ),
    ),
    light = PaletteVariant(
        scheme = lightScheme(
            Accents(
                primary = LimeDeep, onPrimary = Color.White,
                primaryContainer = Color(0xFFE4FF9A), onPrimaryContainer = Color(0xFF1C2400),
                secondary = CyanDeep, onSecondary = Color.White,
                secondaryContainer = Color(0xFFC9F5FF), onSecondaryContainer = Color(0xFF002A33),
                tertiary = AmberDeep, onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFE2B0), onTertiaryContainer = Color(0xFF3A2600),
            ),
        ),
        metrics = MetricColors(
            trace = LimeDeep,
            faster = LimeDeep,
            slower = PinkDeep,
            elevation = AmberDeep,
            heartRate = PinkDeep,
        ),
    ),
)
