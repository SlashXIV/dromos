package com.gabrielifrim.dromos.core.domain.location

import kotlinx.coroutines.flow.Flow

/**
 * Altitude barométrique, en mètres, relative à une pression de référence fixe :
 * seule sa VARIATION a du sens (dénivelé), pas sa valeur absolue.
 * Flux vide sur un appareil sans baromètre.
 */
interface BarometerSource {
    fun altitudes(): Flow<Double>
}
