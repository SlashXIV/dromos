package com.gabrielifrim.dromos.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.gabrielifrim.dromos.core.domain.location.StepSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Podomètre matériel (puce de capteurs, sans service Google) : il compte en
 * intérieur, là où le GPS ne voit rien. Le compteur est cumulé : un événement
 * retardé écran éteint ne perd aucun pas.
 */
internal class StepCounterSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : StepSource {

    override fun steps(): Flow<Int> {
        // Autorisation « activité physique » demandée depuis Android 10 seulement.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (!granted || sensor == null) return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val total = event.values.firstOrNull() ?: return
                    trySend(total.toInt())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            awaitClose { manager.unregisterListener(listener) }
        }
    }
}
