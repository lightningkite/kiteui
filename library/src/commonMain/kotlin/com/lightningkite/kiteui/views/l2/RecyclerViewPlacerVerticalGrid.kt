package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import kotlin.math.abs

@Deprecated("Call directly instead", ReplaceWith("RecyclerViewPlacerVerticalGrid(columns, ratio)"))
fun RecyclerViewPlacerVerticalTrueGrid(columns: Int, ratio: Double = 1.0) = RecyclerViewPlacerVerticalGrid(columns, ratio)
class RecyclerViewPlacerVerticalGrid(val columns: Int, val ratio: Double? = null) :
    RecyclerViewPlacerGrid {
    var log: Console? = null //ConsoleRoot.tag("RecyclerViewPlacerVerticalGrid")
    override fun withOrthogonalCount(count: Int): RecyclerViewPlacerGrid = RecyclerViewPlacerVerticalGrid(count)

    override fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        previousViewport: Rect,
        existingCells: List<RecyclerViewPlaceable>,
        getNewCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
        overdraw: Rect,
        padding: Double,
        spacing: Double,
    ) {
        val cellSize = (viewport.right - viewport.left - padding * 2 - (columns - 1) * spacing) / columns
        val constrain = Size(
            width = cellSize,
            height = 10000.0
        )
        val cellOffsets = (0..<columns).map {
            padding + it * spacing + it * cellSize
        }

        val (anchorRowY, anchorRowIndex) = (anchor?.let {
            when(it) {
                is RecyclerViewAnchor.FuzzyIndex -> {
                    val averageRowHeight = ratio?.let { cellSize * it } ?: existingCells.sumOf { it.bottom - it.top } / existingCells.size
                    val focusRowIndex = it.index.coerceIn(dataRange.first.toDouble(), dataRange.last.toDouble()).div(columns).toInt().times(columns)
                    val partialIndexOffset = it.index.rem(columns) / columns * averageRowHeight
                    viewport.top + viewport.height * it.ratioOfFocus - partialIndexOffset to focusRowIndex
                }
                is RecyclerViewAnchor.SpecificElement -> {
                    val currentIndex = it.index.coerceIn(dataRange).div(columns).times(columns)
                    val cells = (0..<columns).map {
                        if (currentIndex + it in dataRange) getNewCell(
                            currentIndex + it,
                            constrain
                        ) else null
                    }
                    val max = ratio?.let { cellSize * it } ?: cells.maxOf { it?.size?.height ?: 0.0 }
                    when (it.align) {
                        Align.Start -> viewport.top + padding
                        Align.End -> viewport.bottom - max - padding
                        else -> viewport.centerY - max / 2
                    } to currentIndex
                }
            }
        } ?: existingCells.asSequence().filter {
            it.top > overdraw.top
        }.minByOrNull {
            it.centerX +
                    abs(viewport.top - it.top)
        }?.let {
            log?.log("Using existing cells for anchor: ${it.top} to ${it.index.div(columns).times(columns)}")
            it.top to it.index.div(columns).times(columns)
        } ?: run {
            // approximate anchor
            val it = estimateJumpAnchor(existingCells, true, viewport) ?: return@run null
            log?.log("estimateJumpAnchor got $it")
            val averageRowHeight = ratio?.let { cellSize * it } ?: existingCells.sumOf { it.bottom - it.top } / existingCells.size
            val focusRowIndex = it.index.coerceIn(dataRange.first.toDouble(), dataRange.last.toDouble()).div(columns).toInt().times(columns)
            val partialIndexOffset = it.index.rem(columns) / columns * averageRowHeight
            viewport.top + viewport.height * it.ratioOfFocus - partialIndexOffset to focusRowIndex
        } ?: run {
            log?.log("estimateJumpAnchor was dumped; no existin cells?")
            (viewport.top + padding to dataRange.first.div(columns).times(columns))
        }).let {
            // anchor correction for out of bounds
            if(it.second < dataRange.first - columns) {
                log?.log("Anchor ignored due to out-of-range")
                (viewport.top + padding to dataRange.first.div(columns).times(columns))
            } else if(it.second > dataRange.last + columns) {
                log?.log("Anchor ignored due to out-of-range")
                val currentIndex = dataRange.last.div(columns).times(columns)
                val cells = (0..<columns).map {
                    if (currentIndex + it in dataRange) getNewCell(
                        currentIndex + it,
                        constrain
                    ) else null
                }
                val max = ratio?.let { cellSize * it } ?: cells.maxOf { it?.size?.height ?: 0.0 }
                (viewport.bottom - max - padding to currentIndex)
            } else it
        }
        log?.log("ANCHOR $anchorRowY gets index ${anchorRowIndex}")

        // Place downwards, one row at a time
        var currentY = anchorRowY
        var currentIndex = anchorRowIndex
        while (currentY < overdraw.bottom && currentIndex <= dataRange.last) {
            val cells = (0..<columns).map {
                if (currentIndex + it in dataRange) getNewCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = ratio?.let{ cellSize * it } ?: cells.maxOf { it?.size?.height ?: 0.0 }
            for (i in 0..<columns) {
                cells[i]?.place(cellOffsets[i], currentY, cellOffsets[i] + cellSize, currentY + max)
            }
            currentY += max + spacing
            currentIndex += columns
        }
        // Place upwards, one row at a time
        currentY = anchorRowY - spacing
        currentIndex = anchorRowIndex - columns
        while (currentY > overdraw.top && currentIndex + columns - 1 >= dataRange.first) {
            val cells = (0..<columns).map {
                if (currentIndex + it in dataRange) getNewCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = ratio?.let{ cellSize * it } ?: cells.maxOf { it?.size?.height ?: 0.0 }
            for (i in 0..<columns) {
                cells[i]?.place(cellOffsets[i], currentY - max, cellOffsets[i] + cellSize, currentY)
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