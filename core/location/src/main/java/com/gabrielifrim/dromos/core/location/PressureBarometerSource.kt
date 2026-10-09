package com.gabrielifrim.dromos.core.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gabrielifrim.dromos.core.domain.location.BarometerSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Altitude barométrique depuis le capteur de pression.
 *
 * Référence FIXE (atmosphère standard) : la valeur absolue est fausse de
 * quelques dizaines de mètres selon la météo, mais seule la variation sert
 * (le dénivelé), et elle est précise au mètre près.
 */
internal class PressureBarometerSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BarometerSource {

    override fun altitudes(): Flow<Double> {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val hPa = event.values.firstOrNull() ?: return
                    trySend(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hPa).toDouble())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            // ~1 Hz suffit : le moteur n'en garde qu'une valeur par position GPS.
            manager.registerListener(listener, sensor, SAMPLING_US)
            awaitClose { manager.unregisterListener(listener) }
        }
    }

    private companion object {
        const val SAMPLING_US = 1_000_000
    }
}
