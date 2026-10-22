package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import kotlin.test.Test
import kotlin.test.assertEquals

private class PaddingFakePlaceable(override val index: Int) : RecyclerViewPlaceable {
    override val item: Any? = null
    override val size: Size = Size(50.0, 50.0)
    override var left: Double = 0.0
    override var top: Double = 0.0
    override var right: Double = 0.0
    override var bottom: Double = 0.0

    override fun place(left: Double, top: Double, right: Double, bottom: Double) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    override val type: RecyclerViewRenderer<*> = RecyclerViewRenderer.Blank
}

class RecyclerViewPlacerGridPaddingTest {
    private fun RecyclerViewPlacerGrid.placeTwo(paddingTop: Double, paddingLeft: Double): List<PaddingFakePlaceable> {
        val placed = ArrayList<PaddingFakePlaceable>()
        val viewport = Rect(0.0, 0.0, 210.0, 210.0)
        place(
            dataRange = 0..1,
            anchor = null,
            previousViewport = viewport,
            existingCells = listOf(),
            getNewCell = { index, _ -> PaddingFakePlaceable(index).also { placed += it } },
            viewport = viewport,
            overdraw = viewport,
            paddingTop = paddingTop,
            paddingLeft = paddingLeft,
            paddingRight = 0.0,
            paddingBottom = 0.0,
            gap = 0.0,
        )
        return placed.sortedBy { it.index }
    }

    @Test
    fun verticalGridColumnsStartAfterLeftPadding() {
        val cells = RecyclerViewPlacerVerticalGrid(columns = 2).placeTwo(paddingTop = 0.0, paddingLeft = 10.0)
        assertEquals(listOf(10.0, 110.0), cells.map { it.left })
    }

    @Test
    fun horizontalGridRowsStartAfterTopPadding() {
        val cells = RecyclerViewPlacerHorizontalGrid(rows = 2).placeTwo(paddingTop = 10.0, paddingLeft = 0.0)
        assertEquals(listOf(10.0, 110.0), cells.map { it.top })
    }
}
