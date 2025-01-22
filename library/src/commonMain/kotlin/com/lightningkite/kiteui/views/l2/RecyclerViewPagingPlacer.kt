package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.math.abs
import kotlin.math.roundToInt

class RecyclerViewPagingPlacer() : RecyclerViewPlacer {
    var log: Console? = null
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
        existingCells.forEach {
            log?.log("    Index ${it.index} at ${it.left}")
        }
        val (anchorXStart, anchorIndex) = anchor?.let {
            when(it) {
                is RecyclerViewAnchor.FuzzyIndex -> {
                    (viewport.left - it.index.rem(1) * viewport.width) to it.index.toInt()
                }
                is RecyclerViewAnchor.SpecificElement -> viewport.left to it.index
            }
        } ?: existingCells.minByOrNull {
            abs((it.right + it.left) / 2 - previousViewport.centerX)
        }?.let {
            if (previousViewport.width.roundToInt() != viewport.width.roundToInt()) {
                (viewport.centerX + ((it.right + it.left) / 2 - previousViewport.centerX) - viewport.width / 2) to it.index
            } else
                it.left to it.index
        } ?: run {
            viewport.left to dataRange.first
        }
        log?.log("Anchor is ${anchorXStart} index ${anchorIndex}")

        // Place anchor cell
        val size = Size(viewport.width, viewport.height)
        if (anchorIndex in dataRange) {
            getNewCell(anchorIndex, size).place(
                anchorXStart,
                viewport.top,
                anchorXStart + viewport.width,
                viewport.bottom
            )
        }
        if (anchorIndex + 1 in dataRange) {
            getNewCell(anchorIndex + 1, size).place(
                anchorXStart + size.width,
                viewport.top,
                anchorXStart + size.width * 2,
                viewport.bottom
            )
        }
        if (anchorIndex - 1 in dataRange) {
            getNewCell(anchorIndex - 1, size).place(
                anchorXStart - size.width,
                viewport.top,
                anchorXStart,
                viewport.bottom
            )
        }
    }

    override fun prebake(
        prebakeRange: IntRange,
        dataRange: IntRange,
        writer: ViewWriter,
        render: ViewWriter.(Int) -> Unit
    ) {
        TODO("Not yet implemented")
    }
}