package com.gabrielifrim.dromos.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.gabrielifrim.dromos.core.domain.location.LocationSource
import com.gabrielifrim.dromos.core.domain.model.LocationSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import javax.inject.Inject

/**
 * Positions du récepteur GNSS, via le LocationManager de la plateforme.
 *
 * Pas de FusedLocationProvider : il dépend des services Google. Le fournisseur
 * GPS brut suffit en extérieur, c'est exactement là qu'on court.
 */
internal class GnssLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationSource {

    override fun locations(): Flow<LocationSample> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val listener = LocationListenerCompat { location -> trySend(location.toSample()) }
        val request = LocationRequestCompat.Builder(INTERVAL_MS)
            .setMinUpdateIntervalMillis(INTERVAL_MS)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()
        // Permission retirée entre-temps : on ferme le flux en erreur, que le
        // moteur transforme en message à l'écran plutôt qu'en plantage.
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            close(SecurityException("Permission de localisation précise absente"))
            return@callbackFlow
        }
        LocationManagerCompat.requestLocationUpdates(
            manager, LocationManager.GPS_PROVIDER, request, ContextCompat.getMainExecutor(context), listener,
        )
        awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
    }

    private fun Location.toSample() = LocationSample(
        timestamp = Instant.ofEpochMilli(time),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = if (hasAltitude()) altitude else null,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        bearingDegrees = if (hasBearing()) bearing else null,
    )

    private companion object {
        const val INTERVAL_MS = 1_000L
    }
}
