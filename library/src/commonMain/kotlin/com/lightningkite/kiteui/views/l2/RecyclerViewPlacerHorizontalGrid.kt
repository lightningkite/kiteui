package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import kotlin.math.abs

public class RecyclerViewPlacerHorizontalGrid(public val rows: Int) :
    RecyclerViewPlacerGrid {
    override fun withOrthogonalCount(count: Int): RecyclerViewPlacerGrid =
        RecyclerViewPlacerVerticalGrid(count)

    override fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        previousViewport: Rect,
        existingCells: List<RecyclerViewPlaceable>,
        getNewCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
        overdraw: Rect,
        paddingTop: Double,
        paddingLeft: Double,
        paddingRight: Double,
        paddingBottom: Double,
        gap: Double,
    ) {
        val cellSize = (viewport.bottom - viewport.top - paddingTop - paddingBottom - (rows - 1) * gap) / rows
        val constrain = Size(
            height = cellSize,
            width = 10000.0
        )
        // Use DoubleArray to avoid boxing - by Claude
        val cellOffsets = DoubleArray(rows) {
            paddingLeft + it * gap + it * cellSize
        }

        val (anchorRowX, anchorRowIndex) = (anchor?.let {
            when(it) {
                is RecyclerViewAnchor.FuzzyIndex -> {
                    val averageRowWidth = existingCells.sumOf { it.right - it.left } / existingCells.size
                    val focusRowIndex = it.index.coerceIn(dataRange.first.toDouble(), dataRange.last.toDouble()).div(rows).toInt().times(rows)
                    val partialIndexOffset = it.index.rem(rows) / rows * averageRowWidth
                    viewport.left + viewport.width * it.ratioOfFocus - partialIndexOffset to focusRowIndex
                }
                is RecyclerViewAnchor.SpecificElement -> {
                    val currentIndex = it.index.coerceIn(dataRange).div(rows).times(rows)
                    val cells = (0..<rows).map {
                        if (currentIndex + it in dataRange) getNewCell(
                            currentIndex + it,
                            constrain
                        ) else null
                    }
                    val max = cells.maxOf { it?.size?.width ?: 0.0 }
                    when (it.align) {
                        Align.Start -> viewport.left + paddingLeft
                        Align.End -> viewport.right - max - paddingRight
                        else -> viewport.centerX - max / 2
                    } to currentIndex
                }
            }
        } ?: existingCells.asSequence().minByOrNull {
            it.centerY +
                    abs(viewport.left - it.left) +
                    (if(it.left <= overdraw.left) 10000 else 0)
        }?.let {
//            println("Using existing cells for anchor: ${it.left} to ${it.index.div(columns).times(columns)}")
            it.left to it.index.div(rows).times(rows)
        } ?: (viewport.left + paddingLeft to dataRange.first.div(rows).times(rows))).let {
            // anchor correction for out of bounds - existingCells' indices can be stale after
            // the data range shrinks, so re-anchor to the viewport instead of drifting off-screen
            if (it.second < dataRange.first - rows) {
                (viewport.left + paddingLeft to dataRange.first.div(rows).times(rows))
            } else if (it.second > dataRange.last + rows) {
                val currentIndex = dataRange.last.div(rows).times(rows)
                val cells = (0..<rows).map {
                    if (currentIndex + it in dataRange) getNewCell(
                        currentIndex + it,
                        constrain
                    ) else null
                }
                val max = cells.maxOf { it?.size?.width ?: 0.0 }
                (viewport.right - max - paddingRight to currentIndex)
            } else it
        }

        // Place rightwards, one row at a time
        var currentX = anchorRowX
        var currentIndex = anchorRowIndex
        while (currentX < overdraw.right && currentIndex <= dataRange.last) {
            val cells = (0..<rows).map {
                if (currentIndex + it in dataRange) getNewCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = cells.maxOf { it?.size?.width ?: 0.0 }
            for (i in 0..<rows) {
                cells[i]?.place(top = cellOffsets[i], left = currentX, bottom = cellOffsets[i] + cellSize, right = currentX + max)
            }
            currentX += max + gap
            currentIndex += rows
        }
        // Place leftwards, one row at a time
        currentX = anchorRowX - gap
        currentIndex = anchorRowIndex - rows
        while (currentX > overdraw.left && currentIndex + rows - 1 >= dataRange.first) {
            val cells = (0..<rows).map {
                if (currentIndex + it in dataRange) getNewCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = cells.maxOf { it?.size?.width ?: 0.0 }
            for (i in 0..<rows) {
                cells[i]?.place(top = cellOffsets[i], left = currentX - max, bottom = cellOffsets[i] + cellSize, right = currentX)
            }
            currentX -= max + gap
            currentIndex -= rows
        }
    }

    override fun prebake(
        prebakeRange: IntRange,
        dataRange: IntRange,
        writer: ViewWriter,
        render: ViewWriter.(Int) -> Unit
    ): Unit = with(writer) {
        if (rows == 1) {
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