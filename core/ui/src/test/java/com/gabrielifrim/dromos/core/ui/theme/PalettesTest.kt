package com.gabrielifrim.dromos.core.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PalettesTest {

    @Test
    fun `sans choix, la palette par défaut s'applique`() {
        assertThat(Palettes.byId(null)).isEqualTo(Palettes.default)
    }

    @Test
    fun `une palette retirée retombe sur la palette par défaut`() {
        assertThat(Palettes.byId("palette-disparue")).isEqualTo(Palettes.default)
    }

    @Test
    fun `chaque palette se retrouve par son identifiant`() {
        Palettes.all.forEach { assertThat(Palettes.byId(it.id)).isEqualTo(it) }
    }
}
