package com.gabrielifrim.dromos.core.domain.geo

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TraceSimplifierTest {

    @Test
    fun `une ligne droite se réduit à ses deux extrémités`() {
        val line = (0..1000).map { GeoPoint(45.0 + it * 1e-5, 4.0) }
        val simplified = TraceSimplifier.simplify(line, maxPoints = 64)
        assertThat(simplified).containsExactly(line.first(), line.last()).inOrder()
    }

    @Test
    fun `un tracé long tient dans le budget et garde ses coins`() {
        // Un carré parcouru finement : 4 côtés de 500 points.
        val square = buildList {
            repeat(500) { add(GeoPoint(45.0, 4.0 + it * 1e-5)) }
            repeat(500) { add(GeoPoint(45.0 + it * 1e-5, 4.005)) }
            repeat(500) { add(GeoPoint(45.005, 4.005 - it * 1e-5)) }
            repeat(500) { add(GeoPoint(45.005 - it * 1e-5, 4.0)) }
        }
        val simplified = TraceSimplifier.simplify(square, maxPoints = 32)
        assertThat(simplified.size).isAtMost(32)
        // Les trois coins du carré (premiers points des côtés suivants) sont conservés.
        assertThat(simplified).containsAtLeast(GeoPoint(45.0, 4.005), GeoPoint(45.005, 4.005), GeoPoint(45.005, 4.0))
        assertThat(simplified.first()).isEqualTo(square.first())
        assertThat(simplified.last()).isEqualTo(square.last())
    }

    @Test
    fun `un petit tracé est laissé tel quel`() {
        val few = listOf(GeoPoint(1.0, 1.0), GeoPoint(1.1, 1.0), GeoPoint(1.1, 1.2))
        assertThat(TraceSimplifier.simplify(few)).isEqualTo(few)
    }
}
