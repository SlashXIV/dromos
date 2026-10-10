package com.gabrielifrim.dromos.core.domain.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration

class KilometerCounterTest {

    private fun s(seconds: Long) = Duration.ofSeconds(seconds)

    @Test
    fun `rien avant le premier kilomètre`() {
        assertThat(KilometerCounter().onProgress(999.9, s(300))).isNull()
    }

    @Test
    fun `chaque kilomètre est annoncé une fois avec son temps`() {
        val counter = KilometerCounter()
        assertThat(counter.onProgress(1000.0, s(300))).isEqualTo(TrackingEvent.Kilometer(1, s(300), s(0)))
        assertThat(counter.onProgress(1500.0, s(450))).isNull()
        val second = counter.onProgress(2000.0, s(620))!!
        assertThat(second.number).isEqualTo(2)
        assertThat(second.split).isEqualTo(s(320))
    }

    @Test
    fun `un kilomètre plus lent que la moyenne a un écart positif`() {
        val counter = KilometerCounter()
        counter.onProgress(1000.0, s(280))
        // Moyenne sur 2 km : 600 s → 300 s/km ; ce kilomètre : 320 s.
        assertThat(counter.onProgress(2000.0, s(600))!!.versusAverage).isEqualTo(s(20))
    }

    @Test
    fun `plusieurs kilomètres d'un coup se partagent le temps`() {
        val km = KilometerCounter().onProgress(3000.0, s(900))!!
        assertThat(km.number).isEqualTo(3)
        assertThat(km.split).isEqualTo(s(300))
    }

    @Test
    fun `après récupération, rien n'est réannoncé et le premier temps est inconnu`() {
        val counter = KilometerCounter.resumedAt(2400.0)
        assertThat(counter.onProgress(2900.0, s(870))).isNull()
        val km = counter.onProgress(3000.0, s(900))!!
        assertThat(km.number).isEqualTo(3)
        assertThat(km.split).isNull()
        assertThat(km.versusAverage).isNull()
        assertThat(counter.onProgress(4000.0, s(1200))!!.split).isEqualTo(s(300))
    }
}
