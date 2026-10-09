package com.gabrielifrim.dromos.core.domain.model

/**
 * Une sortie restée IN_PROGRESS au démarrage de l'application est une sortie
 * interrompue par la mort du processus : elle est proposée à la reprise.
 */
enum class ActivityStatus { IN_PROGRESS, FINISHED }
