package com.gabrielifrim.dromos.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabrielifrim.dromos.core.data.database.DromosDatabase
import com.gabrielifrim.dromos.core.data.database.MIGRATION_1_2
import com.gabrielifrim.dromos.core.data.database.MIGRATION_2_3
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rejoue une base v1 AUTHENTIQUE, créée par Room depuis le schéma JSON versionné
 * dans `core/data/schemas/` : exactement la base d'un utilisateur de la v0.3.
 * `runMigrationsAndValidate` compare en plus le résultat au schéma v2 attendu.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DromosDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun la_migration_1_2_conserve_sorties_et_points() {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO activity (id, type, status, started_at, ended_at, distance_m, moving_duration_ms,
                    elevation_gain_m, elevation_loss_m, title, notes)
                VALUES (1, 'RUN', 'FINISHED', 1760000000000, 1760003600000, 10012.5, 3120000, 84.0, 81.5,
                    'Sortie du dimanche', NULL)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO track_point (activity_id, timestamp, latitude, longitude, altitude_m, accuracy_m, speed_mps, segment)
                VALUES (1, 1760000000000, 45.75, 4.83, 170.0, 4.0, 3.1, 0)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2)

        db.query("SELECT distance_m, title FROM activity WHERE id = 1").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getDouble(0)).isEqualTo(10012.5)
            assertThat(c.getString(1)).isEqualTo("Sortie du dimanche")
        }
        db.query("SELECT latitude, bearing_deg, baro_altitude_m FROM track_point").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getDouble(0)).isEqualTo(45.75)
            // Les anciens points n'ont ni cap ni baromètre : ils restent calculables sans.
            assertThat(c.isNull(1)).isTrue()
            assertThat(c.isNull(2)).isTrue()
        }
    }

    @Test
    fun la_migration_2_3_suppose_la_pause_automatique_active() {
        helper.createDatabase(databaseName, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO activity (id, type, status, started_at, ended_at, distance_m, moving_duration_ms,
                    elevation_gain_m, elevation_loss_m, title, notes)
                VALUES (7, 'WALK', 'FINISHED', 1760000000000, 1760001800000, 2400.0, 1700000, 12.0, 11.0, NULL, NULL)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(databaseName, 3, true, MIGRATION_2_3)

        db.query("SELECT auto_pause, distance_m FROM activity WHERE id = 7").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getInt(0)).isEqualTo(1)
            assertThat(c.getDouble(1)).isEqualTo(2400.0)
        }
    }
}
