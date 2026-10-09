package com.gabrielifrim.dromos.core.domain.model

/**
 * Apparence choisie par l'utilisateur.
 *
 * [paletteId] est un identifiant opaque : les palettes vivent dans `:core:ui`, le
 * domaine n'a pas à connaître les couleurs. Null = palette par défaut. Un
 * identifiant inconnu (palette retirée d'une version) retombe aussi sur le défaut.
 */
data class AppearanceSettings(
    val paletteId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val motion: MotionPreference = MotionPreference.EXPRESSIVE,
)
