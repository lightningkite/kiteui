package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import kotlin.math.abs

@Deprecated("Call directly instead", ReplaceWith("RecyclerViewPlacerVerticalGrid(columns, ratio)"))
public fun RecyclerViewPlacerVerticalTrueGrid(columns: Int, ratio: Double = 1.0): RecyclerViewPlacerVerticalGrid = RecyclerViewPlacerVerticalGrid(columns, ratio)
public class RecyclerViewPlacerVerticalGrid(
    public val columns: Int,
    public val ratio: Double? = null,
    public val sizeDoesNotChange: Boolean = false,
) :
    RecyclerViewPlacerGrid {
    public val sizeByType: HashMap<RecyclerViewRenderer<*>, Double> = HashMap<RecyclerViewRenderer<*>, Double>()
    public fun RecyclerViewPlaceable.height(cellSize: Double) = ratio?.let { cellSize * it }
        ?: sizeByType[type]
        ?: size.height.also { if(sizeDoesNotChange) sizeByType[type] = it }
    public fun RecyclerViewPlaceable.existingHeight(cellSize: Double) = ratio?.let { cellSize * it }
        ?: sizeByType[type]
        ?: (bottom - top).also { if(sizeDoesNotChange) sizeByType[type] = it }

    var log: Log? = null //ConsoleRoot.tag("RecyclerViewPlacerVerticalGrid")
    override fun withOrthogonalCount(count: Int): RecyclerViewPlacerGrid = RecyclerViewPlacerVerticalGrid(count)

    public override fun place(
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
        val cellSize = (viewport.right - viewport.left - paddingLeft - paddingRight - (columns - 1) * gap) / columns
        val constrain = Size(
            width = cellSize,
            height = 10000.0
        )
        val cellOffsets = (0..<columns).map {
            paddingTop + it * gap + it * cellSize
        }

        val (anchorRowY, anchorRowIndex) = (anchor?.let {
            when(it) {
                is RecyclerViewAnchor.FuzzyIndex -> {
                    val averageRowHeight = existingCells.sumOf { it.existingHeight(cellSize) } / existingCells.size
                    val focusRowIndex = it.index.coerceIn(dataRange.first.toDouble(), dataRange.last.toDouble()).div(columns).toInt().times(columns)
                    val partialIndexOffset = it.index.rem(columns) / columns * (averageRowHeight)
                    viewport.top + (viewport.height - paddingTop - paddingBottom) * it.ratioOfFocus - partialIndexOffset to focusRowIndex
                }
                is RecyclerViewAnchor.SpecificElement -> {
                    val currentIndex = it.index.coerceIn(dataRange).div(columns).times(columns)
                    val cells = (0..<columns).map {
                        if (currentIndex + it in dataRange) getNewCell(
                            currentIndex + it,
                            constrain
                        ) else null
                    }
                    val max = cells.maxOf { it?.height(cellSize) ?: 0.0 }
                    when (it.align) {
                        Align.Start -> viewport.top + paddingTop
                        Align.End -> viewport.bottom - max - paddingBottom
                        else -> viewport.centerY - max / 2
                    } to currentIndex
                }
            }
        } ?: existingCells.asSequence().minByOrNull {
            it.centerX +
                    abs(viewport.top - it.top) +
                    (if(it.top <= overdraw.top) 10000 else 0)
        }?.let {
            log?.log("Using existing cells for anchor: ${it.top} to ${it.index} => ${it.index.div(columns).times(columns)}")
            it.top to it.index.div(columns).times(columns)
        } ?: run {
            // approximate anchor
            val it = estimateJumpAnchor(existingCells, true, viewport) ?: return@run null
            log?.log("estimateJumpAnchor got $it")
            val averageRowHeight = existingCells.sumOf { it.height(cellSize) } / existingCells.size
            val focusRowIndex = it.index.coerceIn(dataRange.first.toDouble(), dataRange.last.toDouble()).div(columns).toInt().times(columns)
            val partialIndexOffset = it.index.rem(columns) / columns * averageRowHeight
            viewport.top + viewport.height * it.ratioOfFocus - partialIndexOffset to focusRowIndex
        } ?: run {
            log?.log("estimateJumpAnchor was dumped; no existin cells?")
            (viewport.top + paddingTop to dataRange.first.div(columns).times(columns))
        }).let {
            // anchor correction for out of bounds
            if(it.second < dataRange.first - columns) {
                log?.log("Anchor ignored due to out-of-range")
                (viewport.top + paddingTop to dataRange.first.div(columns).times(columns))
            } else if(it.second > dataRange.last + columns) {
                log?.log("Anchor ignored due to out-of-range")
                val currentIndex = dataRange.last.div(columns).times(columns)
                val cells = (0..<columns).map {
                    if (currentIndex + it in dataRange) getNewCell(
                        currentIndex + it,
                        constrain
                    ) else null
                }
                val max = cells.maxOf { it?.height(cellSize) ?: 0.0 }
                (viewport.bottom - max - paddingBottom to currentIndex)
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
            val max = cells.maxOf { it?.height(cellSize) ?: 0.0 }
            for (i in 0..<columns) {
                cells[i]?.place(cellOffsets[i], currentY, cellOffsets[i] + cellSize, currentY + max)
            }
            currentY += max + gap
            currentIndex += columns
        }
        // Place upwards, one row at a time
        currentY = anchorRowY - gap
        currentIndex = anchorRowIndex - columns
        while (currentY > overdraw.top && currentIndex + columns - 1 >= dataRange.first) {
            val cells = (0..<columns).map {
                if (currentIndex + it in dataRange) getNewCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = cells.maxOf { it?.height(cellSize) ?: 0.0 }
            for (i in 0..<columns) {
                cells[i]?.place(cellOffsets[i], currentY - max, cellOffsets[i] + cellSize, currentY)
            }
            currentY -= max + gap
            currentIndex -= columns
        }
    }

    public override fun prebake(
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