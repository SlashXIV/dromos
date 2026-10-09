package com.gabrielifrim.dromos.core.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.tracking.TrackingEngine
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Point d'entrée des écrans : pilote le moteur ET garantit que le service de
 * premier plan tourne dès que l'enregistrement est actif. Le service ne doit
 * jamais démarrer avant que le moteur l'exige, ni survivre à la sortie.
 */
@Singleton
class TrackingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val engine: TrackingEngine,
) {
    val state: StateFlow<TrackingState> get() = engine.state

    suspend fun start(type: ActivityType) {
        engine.start(type)
        startService()
    }

    suspend fun pause() = engine.pause()

    suspend fun resume() {
        engine.resume()
        startService()
    }

    suspend fun stop(): Long? = engine.stop()

    suspend fun discard() = engine.discard()

    suspend fun recover(): Boolean = engine.recover()

    private fun startService() {
        ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
    }
}
