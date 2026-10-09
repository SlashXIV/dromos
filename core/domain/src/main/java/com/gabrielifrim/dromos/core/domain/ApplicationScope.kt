package com.gabrielifrim.dromos.core.domain

import javax.inject.Qualifier

/** Portée de coroutines du processus : survit aux écrans, meurt avec l'application. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
