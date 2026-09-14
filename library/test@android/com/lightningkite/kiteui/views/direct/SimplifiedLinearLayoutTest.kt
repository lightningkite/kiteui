package com.lightningkite.kiteui.views.direct

import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

/**
 * Conformance tests for [SimplifiedLinearLayout], locking in the WEIGHT + GAP + GRAVITY
 * measure/layout contract that KiteUI's `row`/`col` implementation depends on (see
 * [com.lightningkite.kiteui.views.direct.RowOrCol] and `modifiers.android.kt`). These exercise
 * real `measure()`/`layout()` passes and assert on the resulting pixel coordinates, so they'd pass
 * against the original, un-pruned AOSP-derived implementation just as well as the trimmed one -
 * they exist to catch a regression in the behavior that's still reachable, not to describe the
 * pruning itself.
 *
 * Every child below uses a fixed (non-WRAP_CONTENT) width and height so its measured size is
 * deterministic without depending on any particular child View's intrinsic content size.
 */
@RunWith(RobolectricTestRunner::class)
class SimplifiedLinearLayoutTest {
    private fun newLayout(orientation: Int): SimplifiedLinearLayout =
        SimplifiedLinearLayout(RuntimeEnvironment.getApplication()).apply {
            this.orientation = orientation
        }

    private fun SimplifiedLinearLayout.addFixedChild(
        width: Int,
        height: Int,
        weight: Float = 0f,
        gravity: Int = -1,
    ): View = View(context).also { child ->
        val lp = SimplifiedLinearLayout.LayoutParams(width, height)
        lp.weight = weight
        lp.gravity = gravity
        addView(child, lp)
    }

    private fun SimplifiedLinearLayout.measureAndLayout(widthSpec: Int, heightSpec: Int) {
        measure(widthSpec, heightSpec)
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
    private fun atMost(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)

    @Test
    fun verticalOrientation_positionsChildrenTopToBottom() {
        val layout = newLayout(SimplifiedLinearLayout.VERTICAL)
        layout.addFixedChild(width = 40, height = 30)
        layout.addFixedChild(width = 50, height = 20)
        layout.addFixedChild(width = 60, height = 10)
        layout.measureAndLayout(exactly(100), atMost(1000))

        assertEquals(0, layout.getChildAt(0).top)
        assertEquals(30, layout.getChildAt(0).bottom)
        assertEquals(30, layout.getChildAt(1).top)
        assertEquals(50, layout.getChildAt(1).bottom)
        assertEquals(50, layout.getChildAt(2).top)
        assertEquals(60, layout.getChildAt(2).bottom)
        assertEquals(60, layout.measuredHeight)
    }

    @Test
    fun horizontalOrientation_positionsChildrenLeftToRight() {
        val layout = newLayout(SimplifiedLinearLayout.HORIZONTAL)
        layout.addFixedChild(width = 30, height = 15)
        layout.addFixedChild(width = 40, height = 15)
        layout.addFixedChild(width = 50, height = 15)
        layout.measureAndLayout(atMost(1000), exactly(100))

        assertEquals(0, layout.getChildAt(0).left)
        assertEquals(30, layout.getChildAt(0).right)
        assertEquals(30, layout.getChildAt(1).left)
        assertEquals(70, layout.getChildAt(1).right)
        assertEquals(70, layout.getChildAt(2).left)
        assertEquals(120, layout.getChildAt(2).right)
        assertEquals(120, layout.measuredWidth)
    }

    @Test
    fun weightDistribution_vertical_splitsRemainingSpaceAmongMixedFixedAndWeightedChildren() {
        val layout = newLayout(SimplifiedLinearLayout.VERTICAL)
        layout.addFixedChild(width = 10, height = 60) // fixed, no weight
        layout.addFixedChild(width = 10, height = 0, weight = 1f)
        layout.addFixedChild(width = 10, height = 0, weight = 2f)
        layout.measureAndLayout(exactly(10), exactly(300))

        // Fixed child keeps its 60px; the remaining 240px splits 1:2 between the weighted children.
        val a = layout.getChildAt(0)
        val b = layout.getChildAt(1)
        val c = layout.getChildAt(2)
        assertEquals(60, a.measuredHeight)
        assertEquals(80, b.measuredHeight)
        assertEquals(160, c.measuredHeight)
        assertEquals(0, a.top); assertEquals(60, a.bottom)
        assertEquals(60, b.top); assertEquals(140, b.bottom)
        assertEquals(140, c.top); assertEquals(300, c.bottom)
    }

    @Test
    fun weightDistribution_horizontal_splitsRemainingSpaceAmongMixedFixedAndWeightedChildren() {
        val layout = newLayout(SimplifiedLinearLayout.HORIZONTAL)
        layout.addFixedChild(width = 50, height = 10) // fixed, no weight
        layout.addFixedChild(width = 0, height = 10, weight = 1f)
        layout.addFixedChild(width = 0, height = 10, weight = 3f)
        layout.measureAndLayout(exactly(250), exactly(10))

        // Fixed child keeps its 50px; the remaining 200px splits 1:3 between the weighted children.
        val a = layout.getChildAt(0)
        val b = layout.getChildAt(1)
        val c = layout.getChildAt(2)
        assertEquals(50, a.measuredWidth)
        assertEquals(50, b.measuredWidth)
        assertEquals(150, c.measuredWidth)
        assertEquals(0, a.left); assertEquals(50, a.right)
        assertEquals(50, b.left); assertEquals(100, b.right)
        assertEquals(100, c.left); assertEquals(250, c.right)
    }

    @Test
    fun gap_vertical_insertsPixelGapBetweenChildrenButNotBeforeFirstOrAfterLast() {
        val layout = newLayout(SimplifiedLinearLayout.VERTICAL)
        layout.gap = 10
        layout.addFixedChild(width = 10, height = 50)
        layout.addFixedChild(width = 10, height = 50)
        layout.addFixedChild(width = 10, height = 50)
        layout.measureAndLayout(exactly(10), atMost(1000))

        // 3 * 50 + 2 * 10 gap = 170; no leading/trailing gap.
        assertEquals(170, layout.measuredHeight)
        assertEquals(0, layout.getChildAt(0).top)
        assertEquals(50, layout.getChildAt(0).bottom)
        assertEquals(60, layout.getChildAt(1).top)
        assertEquals(110, layout.getChildAt(1).bottom)
        assertEquals(120, layout.getChildAt(2).top)
        assertEquals(170, layout.getChildAt(2).bottom)
    }

    @Test
    fun gap_horizontal_insertsPixelGapBetweenChildrenButNotBeforeFirstOrAfterLast() {
        val layout = newLayout(SimplifiedLinearLayout.HORIZONTAL)
        layout.gap = 5
        layout.addFixedChild(width = 30, height = 20)
        layout.addFixedChild(width = 40, height = 20)
        layout.addFixedChild(width = 50, height = 20)
        layout.measureAndLayout(atMost(1000), exactly(20))

        // 30 + 40 + 50 + 2 * 5 gap = 130; no leading/trailing gap.
        assertEquals(130, layout.measuredWidth)
        assertEquals(0, layout.getChildAt(0).left)
        assertEquals(30, layout.getChildAt(0).right)
        assertEquals(35, layout.getChildAt(1).left)
        assertEquals(75, layout.getChildAt(1).right)
        assertEquals(80, layout.getChildAt(2).left)
        assertEquals(130, layout.getChildAt(2).right)
    }

    @Test
    fun gravity_horizontalRow_alignsChildrenOnCrossAxis_startCenterEndAndStretch() {
        // Cross axis for a row is vertical: start=TOP, center=CENTER_VERTICAL, end=BOTTOM,
        // matching what `align(horizontal, vertical)` in modifiers.android.kt sets per child.
        val layout = newLayout(SimplifiedLinearLayout.HORIZONTAL)
        val top = layout.addFixedChild(width = 20, height = 20, gravity = Gravity.TOP)
        val center = layout.addFixedChild(width = 20, height = 30, gravity = Gravity.CENTER_VERTICAL)
        val bottom = layout.addFixedChild(width = 20, height = 40, gravity = Gravity.BOTTOM)
        val stretch = View(layout.context).also {
            val lp = SimplifiedLinearLayout.LayoutParams(20, ViewGroup.LayoutParams.MATCH_PARENT)
            layout.addView(it, lp)
        }
        layout.measureAndLayout(atMost(1000), exactly(100))

        assertEquals(0, top.top)
        assertEquals(35, center.top) // (100 - 30) / 2
        assertEquals(60, bottom.top) // 100 - 40
        assertEquals(0, stretch.top)
        assertEquals(100, stretch.bottom) // MATCH_PARENT fills the cross axis
    }

    @Test
    fun gravity_verticalCol_alignsChildrenOnCrossAxis_startCenterEndAndStretch() {
        // Cross axis for a column is horizontal: start=LEFT, center=CENTER_HORIZONTAL, end=RIGHT
        // (resolved from START/END under the default LTR locale), matching `align(horizontal, vertical)`.
        val layout = newLayout(SimplifiedLinearLayout.VERTICAL)
        val start = layout.addFixedChild(width = 10, height = 20, gravity = Gravity.START)
        val center = layout.addFixedChild(width = 20, height = 20, gravity = Gravity.CENTER_HORIZONTAL)
        val end = layout.addFixedChild(width = 30, height = 20, gravity = Gravity.END)
        val stretch = View(layout.context).also {
            val lp = SimplifiedLinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20)
            layout.addView(it, lp)
        }
        layout.measureAndLayout(exactly(100), atMost(1000))

        assertEquals(0, start.left)
        assertEquals(40, center.left) // (100 - 20) / 2
        assertEquals(70, end.left) // 100 - 30
        assertEquals(0, stretch.left)
        assertEquals(100, stretch.right) // MATCH_PARENT fills the cross axis
    }
}
