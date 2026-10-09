package com.gabrielifrim.dromos.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.gabrielifrim.dromos.core.domain.model.MotionPreference

/**
 * Langage de mouvement de Dromos : trois courbes, reprises du site de l'auteur,
 * et rien d'autre. C'est leur constance, plus que la quantité d'effets, qui
 * donne une identité.
 */
object Motion {
    /** Entrées : départ vif, arrivée douce. */
    val Enter = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

    /** Transitions et balayages : symétrique, appuyé au milieu. */
    val Transition = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    /** Relâchement avec léger rebond (pressions, barres qui poussent). */
    val Release = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1.2f)

    const val ENTER_MS = 500
    const val TRANSITION_MS = 640
    const val RELEASE_MS = 600
    const val DRAW_MS = 1_400

    /** Décalage entre deux lettres qui montent. */
    const val LETTER_STAGGER_MS = 45

    /** Décalage entre deux éléments d'une liste qui entrent. */
    const val ITEM_STAGGER_MS = 60
}

enum class MotionLevel {
    /** Ambiance expressive : tout le mouvement. */
    FULL,

    /** Ambiance sobre : transitions brèves, aucun fond animé ni effet décoratif. */
    REDUCED,

    /** « Supprimer les animations » du système : aucun mouvement. */
    NONE;

    val decorative: Boolean get() = this == FULL
    val animates: Boolean get() = this != NONE
}

val LocalMotionLevel = staticCompositionLocalOf { MotionLevel.FULL }

/**
 * Niveau effectif : le réglage système prime (accessibilité), puis l'ambiance
 * choisie dans Dromos.
 */
@Composable
fun rememberMotionLevel(preference: MotionPreference): MotionLevel {
    val context = LocalContext.current
    val systemScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    return motionLevelOf(systemScale, preference)
}

/** Le réglage système d'accessibilité prime toujours sur l'ambiance choisie. */
fun motionLevelOf(systemAnimatorScale: Float, preference: MotionPreference): MotionLevel = when {
    systemAnimatorScale == 0f -> MotionLevel.NONE
    preference == MotionPreference.CALM -> MotionLevel.REDUCED
    else -> MotionLevel.FULL
}
