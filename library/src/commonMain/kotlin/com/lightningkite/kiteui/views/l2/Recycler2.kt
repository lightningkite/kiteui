package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atBottom
import com.lightningkite.kiteui.views.atEnd
import com.lightningkite.kiteui.views.direct.*
import kotlin.math.abs

interface RecyclerViewPlaceable {
    val index: Int
    val item: Any?
    val size: Size

    val left: Double
    val top: Double
    val right: Double
    val bottom: Double
    val centerX: Double get() = (left + right) / 2
    val centerY: Double get() = (top + bottom) / 2
    fun place(left: Double, top: Double, right: Double, bottom: Double)

    val type: RecyclerViewRenderer<*>
}

interface RecyclerViewRenderer<T> {
    fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>)
}

interface RecyclerViewPlacer {
    fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        existingCells: List<RecyclerViewPlaceable>,
        getNewCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
    )

    fun prebake(
        prebakeRange: IntRange,
        dataRange: IntRange,
        writer: ViewWriter,
        render: ViewWriter.(Int) -> Unit
    )
}

data class RecyclerViewAnchor(
    val index: Int,
    val align: Align
)

class RecyclerViewPlacerVerticalGrid(val columns: Int, val padding: Double, val spacing: Double, val overdraw: Double) :
    RecyclerViewPlacer {
    override fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        existingCells: List<RecyclerViewPlaceable>,
        getCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
    ) {
        val cellSize = (viewport.right - viewport.top) / columns - (columns - 1) * spacing + padding * 2
        val constrain = Size(
            width = cellSize,
            height = 10000.0
        )
        val cellOffsets = (0..<columns).map {
            padding + (it - 1) * spacing + it * cellSize
        }

        val (anchorRowY, anchorRowIndex) = anchor?.let {
            val index = it.index.div(columns).times(columns)
            val cells = (0..<columns).map { getCell(index + it, constrain) }
            val max = cells.maxOf { it.size.height }
            when (it.align) {
                Align.Start -> viewport.top
                Align.End -> viewport.bottom - max
                else -> viewport.centerY - max / 2
            } to index
        } ?: existingCells.minByOrNull {
            it.centerX +
                    abs(viewport.centerY - it.centerY)
        }?.let {
            it.top to it.index
        } ?: (viewport.top + spacing to dataRange.first.div(columns).times(columns))

        // Place downwards, one row at a time
        var currentY = anchorRowY
        var currentIndex = anchorRowIndex
        while (currentY < viewport.bottom + overdraw && currentIndex + columns <= dataRange.last) {
            val cells = (0..<columns).map { getCell(currentIndex + it, constrain) }
            val max = cells.maxOf { it.size.height }
            for (i in 0..<columns) {
                cells[i].place(cellOffsets[i], currentY, cellOffsets[i] + cellSize, currentY + max)
            }
            currentY += max + spacing
            currentIndex += columns
        }
        // Place upwards, one row at a time
        currentY = anchorRowY - spacing
        currentIndex = anchorRowIndex - columns
        while (currentY > viewport.top - overdraw) {
            val cells = (0..<columns).map { getCell(currentIndex + it, constrain) }
            val max = cells.maxOf { it.size.height }
            for (i in 0..<columns) {
                cells[i].place(cellOffsets[i], currentY - max, cellOffsets[i] + cellSize, currentY)
            }
            currentY -= max + spacing
            currentIndex -= columns
        }
    }

    override fun prebake(
        prebakeRange: IntRange,
        dataRange: IntRange,
        writer: ViewWriter,
        render: ViewWriter.(Int) -> Unit
    ): Unit = with(writer) {
        if (columns == 1) {
            col {
                prebakeRange.forEach {
                    render(it)
                }
            }
        } else {
            TODO()
        }
    }
}

class Recycler2(
    viewWriter: ViewWriter,
    vertical: Boolean = true
) {
    private val outerStack: Stack
    private val scroll: ScrollView
    private val cells: ProgrammaticLayout
    private val sentinel: Stack
    private val fakeScroll: ScrollView
    private val fakeScrollContent: ProgrammaticLayout
    private val fakeScrollSentinel: Stack
    init {
        with(viewWriter) {
            stack {
                outerStack = this
                scroll(vertical, !vertical) {
                    scroll = this
                    showScrollBars = false
                    programmatic {
                        cells = this
                        stack {
                            sentinel = this
                        }
                    }
                }
                if (vertical) atEnd - sizeConstraints(width = 0.5.rem)
                else atBottom - sizeConstraints(width = 0.5.rem)
                scroll(vertical, !vertical) {
                    fakeScroll = this
                    programmatic {
                        fakeScrollContent = this
                        stack {
                            fakeScrollSentinel = this
                        }
                    }
                }
            }
        }
    }

    // STARTUP PROCEDURE
    // Await a non-zero scroll view size
    // Starting anchor should be defined at this point; we should have both an index and location set out.  Default to zero/top
    // Run the layout manager's placement routine with the given starting anchor - see below

    // ON SCROLL, RESIZE, NEW DATA, or NEW PLACER
    // Run the layout manager's placement routine with an updated viewport
    //   Pulling a cell should prefer (in order) same item ID, off-screen, create new
    //     Same item ID: Data change should be animated here
    //     Pulling from offscreen
    //       If placing just offscreen, place without animation.
    //       Otherwise, fade in
    //       Data change should be NOT be animated here
    //     Creating a new cell does the same thing as pulling from offscreen
    //   Raw element placement must be deferred to after logic can be run for gluing the first or last cell properly.
    //   Hide unused cells
    //     Fade it out
    //     Post-animation they should be reusable for pulling
    //   If we're showing...
    //     ...the first cell, then we offset the elements to attach the first one to the top.
    //     ...the last cell, then we move the sentinel to match the end of the last cell.
    //     ...neither of the above AND were previously attached to the top, we offset the elements to center them in the huge scroll and move the sentinel very very far down.

    // ON JUMP
    // If the target cell is on screen, perform a simple scrollTo.  Otherwise...
    // Run the layout manager's placement routine with a hard anchor
    //   If scrolling upwards, anchor is the target cell at the top
    //   If scrolling downwards, anchor is the target cell at the bottom
    // Scroll to the newly-created target cell using a simple scrollTo.

}