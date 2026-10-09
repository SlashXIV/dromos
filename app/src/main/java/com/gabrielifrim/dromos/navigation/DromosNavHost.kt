package com.gabrielifrim.dromos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gabrielifrim.dromos.feature.history.HistoryRoute
import com.gabrielifrim.dromos.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

/** Routes typées. Les modules de fonctionnalité ne se connaissent pas : seul ce graphe les relie. */
@Serializable
data object History

@Serializable
data object Settings

@Composable
fun DromosNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = History) {
        composable<History> {
            HistoryRoute(onOpenSettings = { navController.navigate(Settings) })
        }
        composable<Settings> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
