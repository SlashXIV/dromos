package com.gabrielifrim.dromos.core.domain.location

import kotlinx.coroutines.flow.Flow

/**
 * Podomètre matériel : nombre de pas CUMULÉ depuis le démarrage du téléphone.
 * Seule la différence entre deux valeurs a du sens.
 * Flux vide sans capteur ou sans l'autorisation « activité physique ».
 */
interface StepSource {
    fun steps(): Flow<Int>
}
