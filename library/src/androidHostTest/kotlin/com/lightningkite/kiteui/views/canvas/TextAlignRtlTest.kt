package com.lightningkite.kiteui.views.canvas

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * [DrawingContext2D.textAlign]'s start/end values are direction-relative (CSS semantics): under
 * RTL, "start" is the right edge, not the left. [DrawingContext2DImpl.isRtl] is a plain
 * constructor flag - the resolved-layoutDirection plumbing lives in Canvas.android.kt, which needs
 * a real Android View and isn't exercised here - so this pins the pure mapping function itself.
 */
@RunWith(RobolectricTestRunner::class)
class TextAlignRtlTest {
    private fun context(isRtl: Boolean) = DrawingContext2DImpl(android.graphics.Canvas(), isRtl = isRtl)

    @Test
    fun startIsLeftUnderLtr() {
        val ctx = context(isRtl = false)
        ctx.textAlign(TextAlign.start)
        assertEquals(android.graphics.Paint.Align.LEFT, ctx.fillPaintObj.textAlign)
    }

    @Test
    fun endIsRightUnderLtr() {
        val ctx = context(isRtl = false)
        ctx.textAlign(TextAlign.end)
        assertEquals(android.graphics.Paint.Align.RIGHT, ctx.fillPaintObj.textAlign)
    }

    @Test
    fun startIsRightUnderRtl() {
        val ctx = context(isRtl = true)
        ctx.textAlign(TextAlign.start)
        assertEquals(android.graphics.Paint.Align.RIGHT, ctx.fillPaintObj.textAlign)
    }

    @Test
    fun endIsLeftUnderRtl() {
        val ctx = context(isRtl = true)
        ctx.textAlign(TextAlign.end)
        assertEquals(android.graphics.Paint.Align.LEFT, ctx.fillPaintObj.textAlign)
    }

    @Test
    fun explicitLeftAndRightIgnoreDirectionEvenUnderRtl() {
        // TextAlign.left/right are the non-relative variants - direction must not affect them.
        val ctx = context(isRtl = true)
        ctx.textAlign(TextAlign.left)
        assertEquals(android.graphics.Paint.Align.LEFT, ctx.fillPaintObj.textAlign)
        ctx.textAlign(TextAlign.right)
        assertEquals(android.graphics.Paint.Align.RIGHT, ctx.fillPaintObj.textAlign)
    }

    @Test
    fun centerIsUnaffectedByDirection() {
        val ctx = context(isRtl = true)
        ctx.textAlign(TextAlign.center)
        assertEquals(android.graphics.Paint.Align.CENTER, ctx.fillPaintObj.textAlign)
    }
}
