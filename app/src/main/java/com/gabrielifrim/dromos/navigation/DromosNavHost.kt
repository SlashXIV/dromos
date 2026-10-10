package com.gabrielifrim.dromos.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.LocalNavAnimatedScope
import com.gabrielifrim.dromos.core.ui.motion.LocalSharedTransitionScope
import com.gabrielifrim.dromos.core.ui.motion.MotionLevel
import com.gabrielifrim.dromos.core.ui.motion.SCREEN_TRANSITION_MS
import com.gabrielifrim.dromos.core.ui.motion.accentWipe
import com.gabrielifrim.dromos.feature.activity.ActivityDetailRoute
import com.gabrielifrim.dromos.feature.history.HistoryRoute
import com.gabrielifrim.dromos.feature.record.RecordRoute
import com.gabrielifrim.dromos.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

/** Routes typées. Les modules de fonctionnalité ne se connaissent pas : seul ce graphe les relie. */
@Serializable
data object History

@Serializable
data object Settings

@Serializable
data object Record

/** @param justFinished ouverte en fin de sortie : écran de synthèse. */
@Serializable
data class ActivityDetail(val id: Long, val justFinished: Boolean = false)

/**
 * @param trackingActive une sortie est en cours : l'historique propose d'y revenir.
 * @param openOnRecord au lancement, une sortie est en cours ou vient d'être récupérée :
 *   on ouvre directement l'écran de suivi.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DromosNavHost(trackingActive: Boolean, openOnRecord: Boolean) {
    val navController = rememberNavController()
    // Sens de la navigation, lu pendant la composition : le balayage doit le connaître
    // dès la première image de la transition.
    // Retour = l'écran affiché a déjà été visité (sa pile d'avant est retranchée).
    val current by navController.currentBackStackEntryAsState()
    val visited = remember { mutableListOf<String>() }
    // Un State, pas une valeur : l'écran qui part garde les lambdas de son ancien graphe,
    // elles doivent pourtant lire le sens de la navigation en cours.
    val isPop = remember { mutableStateOf(false) }
    current?.id?.let { id ->
        if (visited.lastOrNull() != id) {
            val index = visited.indexOf(id)
            isPop.value = index >= 0
            if (index >= 0) visited.subList(index + 1, visited.size).clear() else visited += id
        }
    }
    // Le balayage dessine lui-même ; les transitions du graphe ne font que tenir les deux
    // écrans composés le temps qu'il passe. Sobre : fondu bref. Sans animations : rien.
    val level = LocalMotionLevel.current
    val enter: EnterTransition = when (level) {
        MotionLevel.FULL -> fadeIn(tween(SCREEN_TRANSITION_MS), initialAlpha = 1f)
        MotionLevel.REDUCED -> fadeIn(tween(REDUCED_MS))
        MotionLevel.NONE -> EnterTransition.None
    }
    val exit: ExitTransition = when (level) {
        MotionLevel.FULL -> fadeOut(tween(SCREEN_TRANSITION_MS), targetAlpha = 1f)
        MotionLevel.REDUCED -> fadeOut(tween(REDUCED_MS))
        MotionLevel.NONE -> ExitTransition.None
    }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = if (openOnRecord) Record else History,
                enterTransition = { enter },
                exitTransition = { exit },
                popEnterTransition = { enter },
                popExitTransition = { exit },
            ) {
                composable<History> {
                    Screen(this, isPop) {
                        HistoryRoute(
                            trackingActive = trackingActive,
                            onOpenRecord = { navController.navigate(Record) { launchSingleTop = true } },
                            onOpenSettings = { navController.navigate(Settings) },
                            onOpenActivity = { navController.navigate(ActivityDetail(it)) },
                        )
                    }
                }
                composable<Settings> {
                    Screen(this, isPop) {
                        SettingsRoute(onBack = { navController.popBackStack() })
                    }
                }
                composable<Record> {
                    Screen(this, isPop) {
                        val backToHistory: () -> Unit = {
                            // Ouvert directement sur le suivi, la pile ne contient pas l'historique.
                            if (!navController.popBackStack(History, inclusive = false)) {
                                navController.navigate(History) { popUpTo(Record) { inclusive = true } }
                            }
                        }
                        // Retour système compris : sans cela, ouvert directement sur le suivi,
                        // il fermerait l'application au lieu de montrer l'historique.
                        BackHandler(onBack = backToHistory)
                        RecordRoute(
                            onBack = backToHistory,
                            onFinished = { id ->
                                if (id == null) {
                                    backToHistory()
                                } else {
                                    // Synthèse à la place du suivi : « retour » ramènera à l'historique.
                                    navController.navigate(ActivityDetail(id, justFinished = true)) {
                                        popUpTo(Record) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }
                }
                composable<ActivityDetail> { entry ->
                    Screen(this, isPop) {
                        val route = entry.toRoute<ActivityDetail>()
                        // Synthèse ouverte depuis un suivi récupéré : rien dessous dans la pile.
                        // Le retour (barre ou système) mène alors à l'historique au lieu de fermer l'appli.
                        val back: () -> Unit = {
                            if (navController.previousBackStackEntry == null) {
                                navController.navigate(History) { popUpTo(0) }
                            } else {
                                navController.popBackStack()
                            }
                        }
                        BackHandler(onBack = back)
                        ActivityDetailRoute(activityId = route.id, justFinished = route.justFinished, onBack = back)
                    }
                }
            }
        }
    }
}

/** Un écran du graphe : sa portée animée pour les éléments partagés, et le balayage d'accent. */
@Composable
private fun Screen(scope: AnimatedContentScope, isPop: State<Boolean>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides scope) {
        Box(Modifier.fillMaxSize().accentWipe(scope, isPop.value)) { content() }
    }
}

private const val REDUCED_MS = 180
