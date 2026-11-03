package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test for the anchor-recovery fix in [RecyclerViewPlacerHorizontalGrid] landed in
 * commit 94f1ebe6b: "anchor correction for out of bounds - existingCells' indices can be stale
 * after the data range shrinks, so re-anchor to the viewport instead of drifting off-screen".
 *
 * `existingCells` can carry an index from before the underlying data shrank. Before the fix,
 * a stale index far outside `dataRange` was fed straight into the place-rightwards/leftwards
 * loops. Both loops terminate the moment `currentX` crosses `overdraw.right`/`overdraw.left`,
 * which - with a large gap - happens on the very first (invalid, `null`-cell) step, so nothing
 * ever got placed. `gap` is set large here specifically to make that failure deterministic
 * rather than "eventually finds the range after N iterations".
 */
private class GridFakePlaceable(override val index: Int) : RecyclerViewPlaceable {
    override val item: Any? = null
    override val size: Size = Size(100.0, 100.0)
    override var left: Double = 0.0
        private set
    override var top: Double = 0.0
        private set
    override var right: Double = 0.0
        private set
    override var bottom: Double = 0.0
        private set

    override fun place(left: Double, top: Double, right: Double, bottom: Double) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    override val type: RecyclerViewRenderer<*> = RecyclerViewRenderer.Blank
}

class L2RecyclerViewPlacerHorizontalGridAnchorRecoveryTest {

    private val viewport = Rect(0.0, 0.0, 100.0, 100.0)

    private fun place(dataRange: IntRange, staleIndex: Int): List<Int> {
        val placer = RecyclerViewPlacerHorizontalGrid(rows = 1)
        val staleCell = GridFakePlaceable(index = staleIndex).apply { place(0.0, 0.0, 100.0, 100.0) }
        val placedIndices = mutableListOf<Int>()
        placer.place(
            dataRange = dataRange,
            anchor = null,
            previousViewport = viewport,
            existingCells = listOf(staleCell),
            getNewCell = { index, _ ->
                placedIndices += index
                GridFakePlaceable(index)
            },
            viewport = viewport,
            overdraw = viewport,
            paddingTop = 0.0,
            paddingLeft = 0.0,
            paddingRight = 0.0,
            paddingBottom = 0.0,
            // Large gap: any invalid (null) step in the old code overshoots overdraw
            // immediately instead of eventually stumbling into the valid range.
            gap = 1000.0,
        )
        return placedIndices
    }

    @Test
    fun staleIndexAboveShrunkDataRange_recoversInsteadOfRenderingNothing() {
        // Data shrank to 0..4; existingCells still reports an item that used to be at index 50.
        val placedIndices = place(dataRange = 0..4, staleIndex = 50)

        // Pre-fix (`git show 94f1ebe6b~1:.../RecyclerViewPlacerHorizontalGrid.kt`): anchorRowIndex
        // stays 50. Rightward loop requires `currentIndex <= dataRange.last` (50 <= 4 is false) so
        // it never runs; leftward loop's first currentX is already <= overdraw.left because of the
        // large gap, so it never runs either. getNewCell is never called - nothing renders.
        assertTrue(placedIndices.isNotEmpty(), "placer should recover cells within dataRange instead of rendering nothing")
        assertTrue(placedIndices.all { it in 0..4 }, "recovered indices must fall back inside dataRange: $placedIndices")
    }

    @Test
    fun staleIndexBelowShrunkDataRange_recoversInsteadOfRenderingNothing() {
        // Data shrank to 10..14; existingCells still reports an item that used to be at index 0.
        val placedIndices = place(dataRange = 10..14, staleIndex = 0)

        // Pre-fix: anchorRowIndex stays 0. The rightward loop's condition (currentIndex <=
        // dataRange.last) is satisfied even for the out-of-range 0, but every cell in 0..9 is
        // `null` (not in dataRange), so `max` is 0 and currentX only advances by `gap` per step;
        // with gap=1000 it overshoots overdraw.right after the very first (index 0) iteration,
        // so getNewCell is never called for anything in the valid range - nothing renders.
        assertTrue(placedIndices.isNotEmpty(), "placer should recover cells within dataRange instead of rendering nothing")
        assertTrue(placedIndices.all { it in 10..14 }, "recovered indices must fall back inside dataRange: $placedIndices")
    }
}
