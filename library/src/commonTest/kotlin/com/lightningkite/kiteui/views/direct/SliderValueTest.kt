package com.lightningkite.kiteui.views.direct

import kotlin.test.Test
import kotlin.test.assertEquals

// The cases follow an HTML range input's value sanitization.
class SliderValueTest {
    @Test
    fun keepsTheValueInRange() {
        assertEquals(0f, sliderValue(-5f, 0f, 10f, null))
        assertEquals(10f, sliderValue(15f, 0f, 10f, null))
        assertEquals(3.3f, sliderValue(3.3f, 0f, 10f, null))
    }

    @Test
    fun aMaxBelowTheMinCountsAsTheMin() {
        assertEquals(18f, sliderValue(0.5f, 18f, 1f, null))
        assertEquals(18f, sliderValue(50f, 18f, 1f, 5f))
    }

    @Test
    fun goesToTheNearestStepFromTheMin() {
        assertEquals(3f, sliderValue(3.4f, 0f, 10f, 1f))
        assertEquals(4f, sliderValue(3.6f, 0f, 10f, 1f))
        assertEquals(3f, sliderValue(3.9f, 1f, 10f, 2f))
        assertEquals(5f, sliderValue(4.1f, 1f, 10f, 2f))
    }

    @Test
    fun halfwayGoesUp() {
        assertEquals(4f, sliderValue(3.5f, 0f, 10f, 1f))
        assertEquals(-3f, sliderValue(-3.5f, -10f, 10f, 1f))
    }

    @Test
    fun neverStepsPastTheMax() {
        // 10 isn't a step from 0 by 3; the last one below it is 9.
        assertEquals(9f, sliderValue(10f, 0f, 10f, 3f))
        assertEquals(9f, sliderValue(9.9f, 0f, 10f, 3f))
    }

    @Test
    fun fractionalStepsDontLoseAStepToFloatError() {
        assertEquals(1f, sliderValue(1f, 0f, 1f, 0.1f), 1e-6f)
        assertEquals(10f, sliderValue(10f, 0.5f, 10f, 0.5f), 1e-6f)
    }
}
