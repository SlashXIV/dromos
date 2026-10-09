package com.gabrielifrim.dromos.core.domain.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoMathTest {

    @Test
    fun `deux points identiques sont à distance nulle`() {
        assertThat(GeoMath.distanceMeters(48.8566, 2.3522, 48.8566, 2.3522)).isEqualTo(0.0)
    }

    @Test
    fun `une minute d'arc de latitude vaut environ un mille marin`() {
        val d = GeoMath.distanceMeters(45.0, 5.0, 45.0 + 1.0 / 60, 5.0)
        assertThat(d).isWithin(2.0).of(1853.2)
    }

    @Test
    fun `Paris - Lyon à vol d'oiseau`() {
        // Notre-Dame de Paris → place Bellecour : ≈ 391,5 km.
        val d = GeoMath.distanceMeters(48.8530, 2.3499, 45.7578, 4.8320)
        assertThat(d / 1000).isWithin(1.5).of(391.5)
    }

    @Test
    fun `la distance est symétrique`() {
        val aller = GeoMath.distanceMeters(43.6, 1.44, 43.61, 1.45)
        val retour = GeoMath.distanceMeters(43.61, 1.45, 43.6, 1.44)
        assertThat(aller).isWithin(1e-9).of(retour)
    }

    @Test
    fun `les antipodes ne produisent pas NaN`() {
        val d = GeoMath.distanceMeters(0.0, 0.0, 0.0, 180.0)
        assertThat(d).isWithin(1.0).of(Math.PI * GeoMath.EARTH_RADIUS_METERS)
    }
}
