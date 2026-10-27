package com.gabrielifrim.dromos.core.domain.analysis

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StepEstimatorTest {

    @Test
    fun `cadences plausibles en marchant et en courant`() {
        // Pas par minute = 60 · v / L(v).
        fun cadence(speed: Double) = 60 * speed / StepEstimator.stepLengthMeters(speed)
        assertThat(cadence(1.0)).isWithin(5.0).of(97.0)
        assertThat(cadence(1.3)).isWithin(5.0).of(111.0)
        assertThat(cadence(3.0)).isWithin(5.0).of(158.0)
        assertThat(cadence(4.0)).isWithin(5.0).of(171.0)
    }

    @Test
    fun `420 m à 1 m par seconde font environ 680 pas`() {
        assertThat(StepEstimator.steps(420.0, 420.0)).isWithin(10.0).of(677.0)
    }

    @Test
    fun `sans durée, l'allure de marche sert de référence`() {
        assertThat(StepEstimator.steps(70.0, 0.0)).isWithin(1.0).of(100.0)
    }
}
