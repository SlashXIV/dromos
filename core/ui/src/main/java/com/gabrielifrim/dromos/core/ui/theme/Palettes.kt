package com.gabrielifrim.dromos.core.ui.theme

import com.gabrielifrim.dromos.core.ui.theme.palettes.CoralPalette
import com.gabrielifrim.dromos.core.ui.theme.palettes.NocturnePalette
import com.gabrielifrim.dromos.core.ui.theme.palettes.SignalPalette

/** Registre des palettes proposées, dans l'ordre d'affichage des Réglages. */
object Palettes {
    val all: List<DromosPalette> = listOf(CoralPalette, NocturnePalette, SignalPalette)

    val default: DromosPalette = CoralPalette

    /** Identifiant inconnu ou absent → palette par défaut, jamais d'écran sans thème. */
    fun byId(id: String?): DromosPalette = all.firstOrNull { it.id == id } ?: default
}
