package com.gabrielifrim.dromos.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.gabrielifrim.dromos.core.ui.R
import com.gabrielifrim.dromos.core.ui.theme.Accents
import com.gabrielifrim.dromos.core.ui.theme.DromosPalette
import com.gabrielifrim.dromos.core.ui.theme.MetricColors
import com.gabrielifrim.dromos.core.ui.theme.PaletteVariant
import com.gabrielifrim.dromos.core.ui.theme.darkScheme
import com.gabrielifrim.dromos.core.ui.theme.lightScheme

private val SignalOrange = Color(0xFFFF6A3D)
private val Glacier = Color(0xFF7FB8D8)
private val Lichen = Color(0xFF9CCB6B)
private val Ochre = Color(0xFFE8C468)
private val Asphalt = Color(0xFF3A3F45)

private val OrangeDeep = Color(0xFFB23A10)
private val GlacierDeep = Color(0xFF2D6A8C)
private val OchreDeep = Color(0xFF85620A)

private val Ink = Color(0xFF111315)

// « Plus rapide / plus lent » : une paire froide / chaude, distincte pour les
// daltoniens (deutéranopie comprise), vérifiée par PaletteContrastTest.
private val FasterBlue = Color(0xFF3D8FD6)
private val SlowerOrange = Color(0xFFE8693F)
private val FasterBlueDeep = Color(0xFF1F6FB2)
private val SlowerOrangeDeep = Color(0xFFB8542A)

/** Palette « Signal » : orange de balisage sur asphalte, plus sobre. */
internal val SignalPalette = DromosPalette(
    id = "signal",
    nameRes = R.string.palette_signal,
    swatches = listOf(SignalOrange, Glacier, Lichen, Ochre, Asphalt),
    dark = PaletteVariant(
        scheme = darkScheme(
            Accents(
                primary = SignalOrange, onPrimary = Ink,
                primaryContainer = Color(0xFF4D2213), onPrimaryContainer = Color(0xFFFFB59C),
                secondary = Glacier, onSecondary = Ink,
                secondaryContainer = Color(0xFF173447), onSecondaryContainer = Color(0xFFC4E3F5),
                tertiary = Ochre, onTertiary = Ink,
                tertiaryContainer = Color(0xFF3B3012), onTertiaryContainer = Ochre,
            ),
        ),
        metrics = MetricColors(
            trace = SignalOrange,
            faster = FasterBlue,
            slower = SlowerOrange,
            elevation = Ochre,
            heartRate = Color(0xFFFF7A8A),
        ),
    ),
    light = PaletteVariant(
        scheme = lightScheme(
            Accents(
                primary = OrangeDeep, onPrimary = Color.White,
                primaryContainer = Color(0xFFFFDBCF), onPrimaryContainer = Color(0xFF3A0B00),
                secondary = GlacierDeep, onSecondary = Color.White,
                secondaryContainer = Color(0xFFCDE6F4), onSecondaryContainer = Color(0xFF0B2A3B),
                tertiary = OchreDeep, onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFBE7B0), onTertiaryContainer = Color(0xFF3A2C00),
            ),
        ),
        metrics = MetricColors(
            trace = OrangeDeep,
            faster = FasterBlueDeep,
            slower = SlowerOrangeDeep,
            elevation = OchreDeep,
            heartRate = Color(0xFFB3263E),
        ),
    ),
)
