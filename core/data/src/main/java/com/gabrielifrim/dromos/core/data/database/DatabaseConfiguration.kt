package com.gabrielifrim.dromos.core.data.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

/** Migrations explicites, dans l'ordre. Vide tant que le schéma est en version 1. */
internal val DROMOS_MIGRATIONS: Array<Migration> = emptyArray()

/**
 * Configuration de la base, définie une seule fois : les tests instrumentés
 * appliquent exactement cette fonction, pas une copie destinée à diverger.
 */
internal fun RoomDatabase.Builder<DromosDatabase>.applyDromosConfiguration():
    RoomDatabase.Builder<DromosDatabase> = this
    // Jamais de fallbackToDestructiveMigration : la base locale est la SEULE copie
    // des sorties. Un schéma non migrable doit échouer bruyamment, pas tout effacer.
    .addMigrations(*DROMOS_MIGRATIONS)
    // WAL explicite : le service écrit des points pendant que l'écran lit l'état,
    // les lecteurs ne doivent jamais attendre un écrivain.
    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    // Un seul écrivain à la fois en WAL : une file d'attente plutôt qu'un SQLITE_BUSY.
    .setTransactionExecutor(Executors.newSingleThreadExecutor())
    .addCallback(object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Supprimer une sortie doit supprimer ses points (ON DELETE CASCADE).
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    })
