package com.gabrielifrim.dromos.core.ui.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.util.Locale

class FormatTest {

    @Test
    fun `les kilomètres prennent la virgule en français`() {
        assertThat(Format.kilometers(10_012.5, Locale.FRANCE)).isEqualTo("10,01")
        assertThat(Format.kilometers(10_012.5, Locale.UK)).isEqualTo("10.01")
    }

    @Test
    fun `la durée n'affiche les heures que si nécessaire`() {
        assertThat(Format.duration(Duration.ofSeconds(52 * 60 + 9))).isEqualTo("52:09")
        assertThat(Format.duration(Duration.ofSeconds(3600 + 5 * 60 + 3))).isEqualTo("1:05:03")
    }

    @Test
    fun `l'allure est en minutes par kilomètre`() {
        // 10 km en 52 min → 5:12 /km.
        assertThat(Format.pace(Duration.ofMinutes(52), 10_000.0)).isEqualTo("5:12")
    }

    @Test
    fun `pas d'allure sans distance`() {
        assertThat(Format.pace(Duration.ofMinutes(3), 0.0)).isNull()
    }
}
