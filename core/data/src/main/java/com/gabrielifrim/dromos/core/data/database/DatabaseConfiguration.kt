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

/**
 * v2 → v3 : la sortie mémorise si la pause automatique était active, pour que
 * l'analyse (splits) retire les mêmes arrêts que le suivi en direct. Les sorties
 * existantes ont toutes été enregistrées avec la pause automatique par défaut.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity ADD COLUMN auto_pause INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * v3 → v4 : aperçu simplifié du tracé, pour les miniatures de l'historique sans
 * relire des milliers de points. Les sorties existantes sont complétées au
 * premier affichage de l'historique (ActivityRepository.backfillPreviews).
 */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity ADD COLUMN preview_polyline TEXT")
    }
}

/**
 * v4 → v5 : pas du podomètre. Les sorties existantes restent à null : le détail
 * les estime depuis la distance et la vitesse.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity ADD COLUMN steps INTEGER")
    }
}

/** Migrations explicites, dans l'ordre. */
internal val DROMOS_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

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
