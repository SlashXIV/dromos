package com.gabrielifrim.dromos.core.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

/** Portée des éléments partagés entre écrans, fournie par le graphe de navigation. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Portée animée de l'écran courant dans la navigation. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Durée des transitions entre écrans. */
const val SCREEN_TRANSITION_MS = 520

/**
 * Balayage d'accent entre écrans : l'écran qui arrive se dévoile de gauche à droite
 * derrière un trait d'accent ; au retour, celui qui part se replie dans l'autre sens.
 * Seul l'écran du dessus balaie, l'autre reste entier dessous : deux balayages
 * simultanés laisseraient un vide entre eux. Expressif seulement.
 *
 * @param isPop la navigation en cours est un retour.
 */
@Composable
fun Modifier.accentWipe(scope: AnimatedVisibilityScope, isPop: Boolean): Modifier {
    if (!LocalMotionLevel.current.decorative) return this
    val accent = MaterialTheme.colorScheme.primary
    val transition = scope.transition
    val entering = transition.targetState == EnterExitState.Visible
    val onTop = if (isPop) !entering else entering
    val reveal by transition.animateFloat(
        transitionSpec = { tween(SCREEN_TRANSITION_MS, easing = Motion.Transition) },
        label = "balayage",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    return drawWithContent {
        if (!onTop || reveal >= 1f) {
            drawContent()
            return@drawWithContent
        }
        val edge = size.width * reveal
        clipRect(right = edge) { this@drawWithContent.drawContent() }
        drawRect(accent, Offset(edge - 4.dp.toPx(), 0f), Size(4.dp.toPx(), size.height))
    }
}

/**
 * Le tracé d'une sortie, partagé entre la vignette de l'historique et la carte du
 * détail : il grandit d'un écran à l'autre. Sans portées ou hors ambiance expressive, rien.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedTrace(activityId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (!LocalMotionLevel.current.decorative) return this
    return with(shared) {
        this@sharedTrace.sharedBounds(
            rememberSharedContentState("trace-$activityId"),
            animated,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp)),
        )
    }
}

/** Vrai pendant la transition d'arrivée ou de départ de l'écran courant. */
@Composable
fun screenTransitionRunning(): Boolean = LocalNavAnimatedScope.current?.transition?.isRunning == true
