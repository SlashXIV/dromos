package com.gabrielifrim.dromos.core.domain.model

data class TrackingPreferences(
    /** Le chrono s'arrête tout seul à l'arrêt (feu rouge, pause photo) et repart au départ. */
    val autoPause: Boolean = true,
)
