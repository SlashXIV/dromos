package com.gabrielifrim.dromos.core.data.mapper

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PreviewCodecTest {

    @Test
    fun `aller-retour à 5 décimales`() {
        val points = listOf(GeoPoint(45.757812, 4.832011), GeoPoint(-33.86882, 151.20929))
        val decoded = PreviewCodec.decode(PreviewCodec.encode(points))!!
        decoded.zip(points).forEach { (a, b) ->
            assertThat(a.latitude).isWithin(1e-5).of(b.latitude)
            assertThat(a.longitude).isWithin(1e-5).of(b.longitude)
        }
    }

    @Test
    fun `absent ou illisible`() {
        assertThat(PreviewCodec.decode(null)).isNull()
        assertThat(PreviewCodec.decode("pas,un;aperçu")).isNull()
        assertThat(PreviewCodec.decode("")).isEmpty()
    }
}
