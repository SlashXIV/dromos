package com.gabrielifrim.dromos.core.domain.model

data class MapPreferences(
    /**
     * Fond de carte téléchargé (tuiles OpenStreetMap via OpenFreeMap). Désactivé :
     * aucune requête réseau, seul le tracé est dessiné.
     */
    val onlineBasemap: Boolean = true,
)
