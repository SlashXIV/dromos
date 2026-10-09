package com.gabrielifrim.dromos.core.domain.location

import com.gabrielifrim.dromos.core.domain.model.LocationSample
import kotlinx.coroutines.flow.Flow

/**
 * Flux de positions. Froid : la collecte démarre le GPS, l'annulation l'arrête.
 * Lève [SecurityException] si la permission de localisation manque.
 */
interface LocationSource {
    fun locations(): Flow<LocationSample>
}
