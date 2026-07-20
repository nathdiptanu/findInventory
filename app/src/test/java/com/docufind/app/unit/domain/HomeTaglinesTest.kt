package com.docufind.app.unit.domain

import com.docufind.app.domain.model.HomeTaglines
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class HomeTaglinesTest {

    @Test
    fun pickRandom_returnsTaglineFromPool() {
        val random = Random(42)
        val (tagline, accent) = HomeTaglines.pickRandom(random)
        assertThat(HomeTaglines.taglines).contains(tagline)
        assertThat(HomeTaglines.accentPalette).contains(accent)
    }

    @Test
    fun taglinePool_hasMinimumVariety() {
        assertThat(HomeTaglines.taglines.size).isAtLeast(50)
    }

    @Test
    fun accentPalette_isNonEmpty() {
        assertThat(HomeTaglines.accentPalette).isNotEmpty()
    }
}
