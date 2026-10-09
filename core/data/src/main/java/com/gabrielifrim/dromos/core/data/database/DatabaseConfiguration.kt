package com.gabrielifrim.dromos.core.data.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

/**
 * v1 → v2 : cap GNSS et altitude barométrique par point (jalon « Métriques
 * fiables »). Colonnes nullables : les points existants restent valides, ils
 * seront simplement calculés sans Doppler ni baromètre.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE track_point ADD COLUMN bearing_deg REAL")
        db.execSQL("ALTER TABLE track_point ADD COLUMN baro_altitude_m REAL")
    }
}

/** Migrations explicites, dans l'ordre. */
internal val DROMOS_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

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
