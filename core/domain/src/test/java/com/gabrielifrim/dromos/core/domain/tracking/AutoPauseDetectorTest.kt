package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.tracking.AutoPauseDetector.Decision
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class AutoPauseDetectorTest {

    private val t0 = Instant.parse("2026-10-10T08:00:00Z")
    private fun at(s: Long) = t0.plusSeconds(s)

    @Test
    fun `l'arrêt est confirmé après 5 s et daté du premier instant lent`() {
        val d = AutoPauseDetector(ActivityType.RUN)
        assertThat(d.onSpeed(at(0), 3.0, autoPaused = false)).isEqualTo(Decision.None)
        (1L..5L).forEach { assertThat(d.onSpeed(at(it), 0.2, autoPaused = false)).isEqualTo(Decision.None) }
        assertThat(d.onSpeed(at(6), 0.1, autoPaused = false)).isEqualTo(Decision.Stop(at(1)))
    }

    @Test
    fun `un ralentissement bref ne déclenche rien`() {
        val d = AutoPauseDetector(ActivityType.RUN)
        (0L..3L).forEach { d.onSpeed(at(it), 0.2, autoPaused = false) }
        d.onSpeed(at(4), 3.0, autoPaused = false)
        (5L..8L).forEach { assertThat(d.onSpeed(at(it), 0.2, autoPaused = false)).isEqualTo(Decision.None) }
    }

    @Test
    fun `le départ est confirmé après 2 s au-dessus du seuil haut`() {
        val d = AutoPauseDetector(ActivityType.RUN)
        assertThat(d.onSpeed(at(0), 2.0, autoPaused = true)).isEqualTo(Decision.None)
        assertThat(d.onSpeed(at(1), 2.0, autoPaused = true)).isEqualTo(Decision.None)
        assertThat(d.onSpeed(at(2), 2.0, autoPaused = true)).isEqualTo(Decision.Go(at(0)))
    }

    @Test
    fun `entre les deux seuils, l'état ne change pas`() {
        val d = AutoPauseDetector(ActivityType.RUN)
        (0L..20L).forEach { assertThat(d.onSpeed(at(it), 1.0, autoPaused = true)).isEqualTo(Decision.None) }
        (0L..20L).forEach { assertThat(d.onSpeed(at(it), 1.0, autoPaused = false)).isEqualTo(Decision.None) }
    }

    @Test
    fun `un marcheur lent n'est pas mis en pause`() {
        val d = AutoPauseDetector(ActivityType.WALK)
        (0L..60L).forEach { assertThat(d.onSpeed(at(it), 0.7, autoPaused = false)).isEqualTo(Decision.None) }
    }
}
