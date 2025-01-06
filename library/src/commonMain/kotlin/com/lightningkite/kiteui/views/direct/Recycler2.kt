package com.lightningkite.kiteui.views.direct
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.math.abs

interface RecyclerViewPlaceable {
    val index: Int
    val item: Any?
    fun measure(constrainTo: Size): Size

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
        render: ViewWriter.(Int)->Unit
    )
}
data class RecyclerViewAnchor(
    val index: Int,
    val align: Align
)

class RecyclerViewPlacerVerticalGrid(val columns: Int, val padding: Double, val spacing: Double, val overdraw: Double): RecyclerViewPlacer {
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
            val max = cells.maxOf { it.measure(constrain).height }
            when(it.align) {
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
        while(currentY < viewport.bottom + overdraw && currentIndex + columns <= dataRange.last) {
            val cells = (0..<columns).map { getCell(currentIndex + it, constrain) }
            val max = cells.maxOf { it.measure(constrain).height }
            for(i in 0..<columns) {
                cells[i].place(cellOffsets[i], currentY, cellOffsets[i] + cellSize, currentY + max)
            }
            currentY += max + spacing
            currentIndex += columns
        }
        // Place upwards, one row at a time
        currentY = anchorRowY - spacing
        currentIndex = anchorRowIndex - columns
        while(currentY > viewport.top - overdraw) {
            val cells = (0..<columns).map { getCell(currentIndex + it, constrain) }
            val max = cells.maxOf { it.measure(constrain).height }
            for(i in 0..<columns) {
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
        render: ViewWriter.(Int)->Unit
    ): Unit = with(writer) {
        if(columns == 1) {
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