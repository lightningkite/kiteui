package com.lightningkite.kiteui.paint

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.applyAlpha
import com.lightningkite.kiteui.models.darken
import com.lightningkite.kiteui.models.lighten
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class PaintTest {

    @Test
    fun color_map_appliesTransformation() = runTest {
        val color = Color(1f, 0.2f, 0.3f, 0.4f)

        val result = color.map { it.copy(red = 1f) } as Color

        assertEquals(1f, result.red)
        assertEquals(0.3f, result.green)
        assertEquals(0.4f, result.blue)
    }

    @Test
    fun color_closestColor_returnsSelf() = runTest {
        val color = Color.red

        assertEquals(color, color.closestColor())
    }

    @Test
    fun paint_applyAlpha_changesAlpha() = runTest {
        val color: Paint = Color(alpha = 1f, red = 1f)

        val result = color.applyAlpha(0.5f).closestColor()

        assertEquals(0.5f, result.alpha)
    }

    @Test
    fun paint_lighten_changesColor() = runTest {
        val color: Paint = Color(1f, 0f, 0f, 0f)

        val result = color.lighten(0.5f).closestColor()

        assertEquals(0.5f, result.red)
        assertEquals(0.5f, result.green)
        assertEquals(0.5f, result.blue)
    }

    @Test
    fun paint_darken_changesColor() = runTest {
        val color: Paint = Color(1f, 1f, 1f, 1f)

        val result = color.darken(0.5f).closestColor()

        assertEquals(0.5f, result.red)
        assertEquals(0.5f, result.green)
        assertEquals(0.5f, result.blue)
    }
}