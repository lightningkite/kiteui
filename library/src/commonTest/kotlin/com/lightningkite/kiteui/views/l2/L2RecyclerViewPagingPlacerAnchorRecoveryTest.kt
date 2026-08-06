package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for the anchor-recovery fix in [RecyclerViewPagingPlacer] landed in
 * commit 94f1ebe6b. `existingCells` can carry an index computed on a previous (larger)
 * `dataRange` - e.g. after the underlying list shrinks - and before the fix, an anchor
 * index outside `dataRange` was used as-is. `place()` only ever draws `anchorIndex`,
 * `anchorIndex + 1` and `anchorIndex - 1`, each guarded by `in dataRange`, so an
 * out-of-range anchor made every one of those checks fail and the placer silently
 * rendered nothing for that frame.
 */
private class FakePlaceable(override val index: Int) : RecyclerViewPlaceable {
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

class L2RecyclerViewPagingPlacerAnchorRecoveryTest {

    @Test
    fun staleExistingCellIndexOutsideShrunkDataRange_recoversInsteadOfRenderingNothing() {
        val placer = RecyclerViewPagingPlacer()
        val viewport = Rect(0.0, 0.0, 100.0, 100.0)

        // existingCells carries index 50, left over from before the list shrank to 5 items (0..4).
        val staleCell = FakePlaceable(index = 50).apply { place(0.0, 0.0, 100.0, 100.0) }

        val placedIndices = mutableListOf<Int>()
        placer.place(
            dataRange = 0..4,
            anchor = null,
            previousViewport = viewport,
            existingCells = listOf(staleCell),
            getNewCell = { index, _ ->
                placedIndices += index
                FakePlaceable(index)
            },
            viewport = viewport,
            overdraw = viewport,
            paddingTop = 0.0,
            paddingLeft = 0.0,
            paddingRight = 0.0,
            paddingBottom = 0.0,
            gap = 0.0,
        )

        // Before the fix, anchorIndex stayed 50: 50, 51 and 49 are all outside 0..4, so
        // getNewCell was never called (verified by reading the pre-fix code at
        // `git show 94f1ebe6b~1:library/src/commonMain/kotlin/com/lightningkite/kiteui/views/l2/RecyclerViewPagingPlacer.kt`,
        // which lacks the trailing `.let { if (it.second !in dataRange) ... }` correction).
        // After the fix the anchor falls back to dataRange.first (0), so the placer recovers
        // and renders index 0 (plus its neighbor 1, since -1 is out of range).
        assertEquals(listOf(0, 1), placedIndices)
    }
}
