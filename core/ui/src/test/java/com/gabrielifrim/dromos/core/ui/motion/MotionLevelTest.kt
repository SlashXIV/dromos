package com.gabrielifrim.dromos.core.ui.motion

import com.gabrielifrim.dromos.core.domain.model.MotionPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MotionLevelTest {

    @Test
    fun `l'ambiance expressive donne tout le mouvement`() {
        assertThat(motionLevelOf(1f, MotionPreference.EXPRESSIVE)).isEqualTo(MotionLevel.FULL)
    }

    @Test
    fun `l'ambiance sobre réduit le mouvement et coupe le décor`() {
        val level = motionLevelOf(1f, MotionPreference.CALM)
        assertThat(level).isEqualTo(MotionLevel.REDUCED)
        assertThat(level.decorative).isFalse()
        assertThat(level.animates).isTrue()
    }

    @Test
    fun `« Supprimer les animations » prime sur l'ambiance`() {
        val level = motionLevelOf(0f, MotionPreference.EXPRESSIVE)
        assertThat(level).isEqualTo(MotionLevel.NONE)
        assertThat(level.animates).isFalse()
    }
}
