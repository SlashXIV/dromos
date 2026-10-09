package com.gabrielifrim.dromos.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

/**
 * @param trackingActive une sortie est en cours : l'historique propose d'y revenir.
 * @param openOnRecord au lancement, une sortie est en cours ou vient d'être récupérée :
 *   on ouvre directement l'écran de suivi.
 */
@Composable
fun DromosNavHost(trackingActive: Boolean, openOnRecord: Boolean) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = if (openOnRecord) Record else History) {
        composable<History> {
            HistoryRoute(
                trackingActive = trackingActive,
                onOpenRecord = { navController.navigate(Record) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Settings) },
            )
        }
        composable<Settings> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable<Record> {
            val backToHistory: () -> Unit = {
                // Ouvert directement sur le suivi, la pile ne contient pas l'historique.
                if (!navController.popBackStack(History, inclusive = false)) {
                    navController.navigate(History) { popUpTo(Record) { inclusive = true } }
                }
            }
            // Retour système compris : sans cela, ouvert directement sur le suivi,
            // il fermerait l'application au lieu de montrer l'historique.
            BackHandler(onBack = backToHistory)
            RecordRoute(onBack = backToHistory, onFinished = backToHistory)
        }
    }
}
