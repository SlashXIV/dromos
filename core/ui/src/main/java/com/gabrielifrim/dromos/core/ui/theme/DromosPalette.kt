package com.gabrielifrim.dromos.core.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Une palette complète : ses déclinaisons claire et sombre, et ses couleurs
 * d'origine pour l'aperçu dans les Réglages.
 *
 * Ajouter une palette = écrire un fichier dans `palettes/` et l'inscrire dans
 * [Palettes.all]. `PaletteContrastTest` vérifie alors automatiquement sa lisibilité.
 */
@Immutable
data class DromosPalette(
    /** Identifiant persisté dans les réglages : ne jamais le renommer. */
    val id: String,
    @StringRes val nameRes: Int,
    /** Couleurs sources, dans l'ordre où elles sont présentées. */
    val swatches: List<Color>,
    val light: PaletteVariant,
    val dark: PaletteVariant,
)

@Immutable
data class PaletteVariant(
    val scheme: ColorScheme,
    val metrics: MetricColors,
)
