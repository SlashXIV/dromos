package com.gabrielifrim.dromos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.ui.motion.rememberMotionLevel
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.theme.Palettes
import com.gabrielifrim.dromos.navigation.DromosNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Activité unique : elle porte le thème et le graphe de navigation, rien d'autre. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Avant super.onCreate : le thème de démarrage doit être remplacé pendant
        // que la fenêtre se crée. Il reste affiché jusqu'à la lecture des réglages,
        // pour ne jamais montrer un flash de la mauvaise palette.
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.appearance.value == null || !viewModel.recoveryChecked.value
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val recoveryChecked by viewModel.recoveryChecked.collectAsStateWithLifecycle()
            val tracking by viewModel.trackingState.collectAsStateWithLifecycle()
            val settings = appearance ?: return@setContent
            if (!recoveryChecked) return@setContent
            // Lu une seule fois : la destination de départ ne doit pas changer en cours de route.
            val openOnRecord = remember { tracking is TrackingState.Active }
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Les icônes de la barre d'état suivent le thème de l'APPLICATION : en
            // « Sombre » forcé sur un système clair, elles seraient sinon sombres sur sombre.
            LaunchedEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val motion = rememberMotionLevel(settings.motion)
            DromosTheme(palette = Palettes.byId(settings.paletteId), darkTheme = dark, motion = motion) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DromosNavHost(trackingActive = tracking is TrackingState.Active, openOnRecord = openOnRecord)
                }
            }
        }
    }
}
