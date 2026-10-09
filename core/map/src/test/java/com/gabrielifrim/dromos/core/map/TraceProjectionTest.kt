package com.gabrielifrim.dromos.core.map

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TraceProjectionTest {

    @Test
    fun `le tracé tient dans le cadre, nord en haut`() {
        val south = GeoPoint(45.0, 4.0)
        val north = GeoPoint(45.01, 4.0)
        val p = TraceProjection(listOf(listOf(south, north)), width = 400f, height = 400f, padding = 20f)
        assertThat(p.y(north)).isLessThan(p.y(south))
        listOf(south, north).forEach {
            assertThat(p.x(it)).isIn(com.google.common.collect.Range.closed(20f, 380f))
            assertThat(p.y(it)).isIn(com.google.common.collect.Range.closed(20f, 380f))
        }
    }

    @Test
    fun `même échelle sur les deux axes`() {
        // Un carré de 1 km de côté reste un carré à l'écran, malgré la latitude.
        val lat = 45.0
        val dLat = 1000 / 111_195.0
        val dLon = dLat / kotlin.math.cos(Math.toRadians(lat))
        val sw = GeoPoint(lat, 4.0)
        val ne = GeoPoint(lat + dLat, 4.0 + dLon)
        val p = TraceProjection(listOf(listOf(sw, ne)), width = 600f, height = 300f, padding = 0f)
        val w = p.x(ne) - p.x(sw)
        val h = p.y(sw) - p.y(ne)
        assertThat(w.toDouble()).isWithin(1.0).of(h.toDouble())
    }

    @Test
    fun `un point unique ne divise pas par zéro`() {
        val p = TraceProjection(listOf(listOf(GeoPoint(45.0, 4.0))), 100f, 100f, 10f)
        assertThat(p.x(GeoPoint(45.0, 4.0)).isFinite()).isTrue()
    }
}
