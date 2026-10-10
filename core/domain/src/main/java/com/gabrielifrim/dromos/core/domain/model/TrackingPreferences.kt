package com.gabrielifrim.dromos.core.domain.model

data class TrackingPreferences(
    /** Le chrono s'arrête tout seul à l'arrêt (feu rouge, pause photo) et repart au départ. */
    val autoPause: Boolean = true,
    /** « Ne plus demander » choisi sur la fenêtre d'optimisation de batterie. */
    val batteryPromptDismissed: Boolean = false,
    /** Compte à rebours 3-2-1 avant le départ. */
    val countdown: Boolean = true,
)
