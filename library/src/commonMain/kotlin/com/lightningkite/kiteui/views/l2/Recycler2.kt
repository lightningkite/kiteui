package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.launch
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

interface RecyclerViewData<T, ID> {
    val range: IntRange
    operator fun get(index: Int): T

    object Empty : RecyclerViewData<Unit, Unit> {
        override val range: IntRange get() = IntRange.EMPTY
        override fun get(index: Int): Unit = Unit
    }
}

interface RecyclerViewRendererSet<in T, out ID> {
    fun id(item: T): ID
    fun renderer(item: T): RecyclerViewRenderer<T>

    object Empty : RecyclerViewRendererSet<Any?, Unit> {
        override fun id(item: Any?): Unit = Unit
        override fun renderer(item: Any?): RecyclerViewRenderer<Any?> = RecyclerViewRenderer.Blank
    }
}

interface RecyclerViewRenderer<in T> {
    fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>)

    object Blank : RecyclerViewRenderer<Any?> {
        override fun render(viewWriter: ViewWriter, data: Readable<Any?>, index: Readable<Int>) {
            with(viewWriter) { space() }
        }
    }
}

interface RecyclerViewPlacer {
    val scrollBeyond: Double

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

    fun inBounds(left: Double, top: Double, right: Double, bottom: Double, viewport: Rect): Boolean
}

data class RecyclerViewAnchor(
    val index: Int,
    val align: Align
)

class RecyclerViewPlacerVerticalGrid(val columns: Int, val padding: Double, val spacing: Double, val overdraw: Double) :
    RecyclerViewPlacer {
    override val scrollBeyond: Double
        get() = padding

    override fun inBounds(left: Double, top: Double, right: Double, bottom: Double, viewport: Rect): Boolean {
        return rectOverlaps(
            left,
            top,
            right,
            bottom,
            viewport.left - overdraw,
            viewport.top - overdraw,
            viewport.right + overdraw,
            viewport.bottom + overdraw
        )
    }

    override fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        existingCells: List<RecyclerViewPlaceable>,
        getCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
    ) {
        val cellSize = (viewport.right - viewport.left - padding * 2 - (columns - 1) * spacing) / columns
        val constrain = Size(
            width = cellSize,
            height = 10000.0
        )
        val cellOffsets = (0..<columns).map {
            padding + it * spacing + it * cellSize
        }

        val (anchorRowY, anchorRowIndex) = anchor?.let {
            val index = it.index.div(columns).times(columns)
            val cells = (0..<columns).map { getCell(index + it, constrain) }
            val max = cells.maxOf { it.size.height }
            when (it.align) {
                Align.Start -> viewport.top + padding
                Align.End -> viewport.bottom - max - padding
                else -> viewport.centerY - max / 2
            } to index
        } ?: existingCells.minByOrNull {
            it.centerX +
                    abs(viewport.centerY - it.centerY)
        }?.let {
            it.top to it.index.div(columns).times(columns)
        } ?: (viewport.top + spacing to dataRange.first.div(columns).times(columns))

        // Place downwards, one row at a time
        var currentY = anchorRowY
        var currentIndex = anchorRowIndex
        while (currentY < viewport.bottom + overdraw && currentIndex <= dataRange.last) {
            val cells = (0..<columns).map {
                if (currentIndex + it in dataRange) getCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = cells.maxOf { it?.size?.height ?: 0.0 }
            for (i in 0..<columns) {
                cells[i]?.place(cellOffsets[i], currentY, cellOffsets[i] + cellSize, currentY + max)
            }
            currentY += max + spacing
            currentIndex += columns
        }
        // Place upwards, one row at a time
        currentY = anchorRowY - spacing
        currentIndex = anchorRowIndex - columns
        while (currentY > viewport.top - overdraw && currentIndex + columns - 1 >= dataRange.first) {
            val cells = (0..<columns).map {
                if (currentIndex + it in dataRange) getCell(
                    currentIndex + it,
                    constrain
                ) else null
            }
            val max = cells.maxOf { it?.size?.height ?: 0.0 }
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

class Recycler2(
    viewWriter: ViewWriter,
    vertical: Boolean = true,
    val log: Console? = ConsoleRoot.tag("Recycler2")
) {
    private val outerStack: Stack
    private val scroll: ScrollView
    private val cells: ProgrammaticLayout
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

    private var anchor: RecyclerViewAnchor? = RecyclerViewAnchor(0, Align.Start)

    var placer: RecyclerViewPlacer = RecyclerViewPlacerVerticalGrid(1, 8.0, 8.0, 8.0)
        set(value) {
            field = value; cells.invalidateLayout()
        }
    var data: RecyclerViewData<*, *>? = RecyclerViewData.Empty
        set(value) {
            field = value
            cells.invalidateLayout()
        }
    var rendererSet: RecyclerViewRendererSet<*, *>? = RecyclerViewRendererSet.Empty
        set(value) {
            field = value
            cells.clearChildren()
            activeCells = ArrayList()
            reuseableCells = ArrayList()
            cells.invalidateLayout()
        }

    private var activeCells = ArrayList<MyCell<*>>()
    private var reuseableCells = ArrayList<MyCell<*>>()

    fun scrollToIndex(toIndex: Int, align: Align, animate: Boolean = true) {
        anchor = RecyclerViewAnchor(toIndex, align)
        cells.invalidateLayout()
    }

    private inner class MyCell<T> : RecyclerViewPlaceable {
        val indexProp = Property(-1)
        val data = LateInitProperty<T>()
        override lateinit var type: RecyclerViewRenderer<*>
        lateinit var view: RView
        fun setup(
            type: RecyclerViewRenderer<T>,
            constrain: Size,
            data: T?,
            index: Int,
            inProgress: ProgrammingLayoutInProgress
        ) {
            cells.beforeNextElementSetup { view = this }
            data?.let { this.data.value = it } ?: this.data.unset()
            this.indexProp.value = index
            type.render(cells, this.data, indexProp)
            size = inProgress.measure(view, constrain)
        }

        fun onPullForPlacing(constrain: Size, data: T?, index: Int, inProgress: ProgrammingLayoutInProgress) {
            view.withoutAnimation {
                view.exists = true
                view.opacity = 1.0
            }
            if(data != this.data.value) log?.log("CELL RECYCLED: Change from ${this.data.value} to $data at $index, ${this.indexProp.value}")
            data?.let { this.data.value = it } ?: this.data.unset()
            this.indexProp.value = index
            size = inProgress.measure(view, constrain)
        }

        fun animatedDismiss() {
            view.opacity = 0.0
            val reuse = reuseableCells
            activeCells.remove(this)
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds) {
                view.exists = false
                reuse.add(this@MyCell)
            }
        }

        fun instantDismiss() {
            view.exists = false
            activeCells.remove(this)
            reuseableCells.add(this@MyCell)
        }

        override val index: Int get() = indexProp.value
        override val item: Any? get() = data.state.getOrNull()
        override var size: Size = Size(0.0, 0.0)
        override var left: Double = 0.0
        override var top: Double = 0.0
        override var right: Double = 0.0
        override var bottom: Double = 0.0

        var leftNew: Double = 0.0
        var topNew: Double = 0.0
        var rightNew: Double = 0.0
        var bottomNew: Double = 0.0

        var leftOld: Double = 0.0
        var topOld: Double = 0.0
        var rightOld: Double = 0.0
        var bottomOld: Double = 0.0

        override fun place(left: Double, top: Double, right: Double, bottom: Double) {
            leftNew = left
            topNew = top
            rightNew = right
            bottomNew = bottom
        }

        fun offset(x: Double, y: Double) {
            left += x
            top += y
            right += x
            bottom += y
            leftNew += x
            topNew += y
            rightNew += x
            bottomNew += y
        }
    }

    val reallyBig = 10_000.0
    var inLayout: Boolean = false

    init {
        scroll.onRemove(scroll.viewport.addListener {
            if (!inLayout) cells.invalidateLayout()
        })
    }

    val programmaticLayoutDelegate = object : ProgrammaticLayoutDelegate {
        var queuedScrollOffset: Pair<Double, Double>? = null
        var viewport: Rect = Rect.Zero
        var lastMeasure: Size? = null
        var stahp = false
        override fun measure(
            layout: ProgrammaticLayout,
            inProgress: ProgrammingLayoutInProgress,
            within: Size
        ): Size {
            if(stahp) return lastMeasure!!
            val default = within
            if(within == Size.Zero) {
                println("Cannot measure yet: within == Size.Zero")
                return default
            }
            viewport = scroll.viewport.state.getOrNull() ?: run {
                println("Cannot measure yet:  scroll.viewport.state.getOrNull()  == null")
                return default
            }
            if (viewport.width == 0.0 || viewport.height == 0.0) {
                println("Cannot measure yet: viewport.width == 0.0 || viewport.height == 0.0 ($viewport)")
                return default
            }
            if(queuedScrollOffset != null) {
                IllegalStateException("measure while queuedScrollOffset != null").printStackTrace()
                return lastMeasure!!
            }
            log?.log("LAYOUT STARTING with size $within")

            @Suppress("UNCHECKED_CAST")
            val data = data as? RecyclerViewData<Any, Any> ?: return default

            @Suppress("UNCHECKED_CAST")
            val rendererSet = rendererSet as? RecyclerViewRendererSet<Any, Any> ?: return default

            @Suppress("UNCHECKED_CAST")
            val activeCells = activeCells as? MutableList<MyCell<Any>> ?: return default

            @Suppress("UNCHECKED_CAST")
            val reuseableCells = reuseableCells as? MutableList<MyCell<Any>> ?: return default
            log?.log("LAYOUT STARTED IN VIEWPORT $viewport")

            // Just nuke the offscreen cells immediately.
            val instantDismissCount = activeCells.toList().count {
                if(!placer.inBounds(it.left, it.top, it.right, it.bottom, viewport)){
                    it.instantDismiss()
                    true
                } else false
            }
            log?.log("OFFSCREEN CELLS DISMISSED: ${instantDismissCount}")

            // Time to run the placer.
            //  Track the used cells so that we can handle them properly later
            val usedCells = HashSet<MyCell<*>>()

            activeCells.toSet().intersect(reuseableCells.toSet()).forEach {
                log?.log("WARNING!!! Active and reusable cell $it")
            }

            fun runPlacer() {
                log?.log("RUN PLACER IN $viewport")
                placer.place(
                    dataRange = data.range,
                    anchor = anchor,
                    existingCells = activeCells,
                    getNewCell = { index, size ->
                        val item = data[index]
                        val id = rendererSet.id(item)
                        //Pulling a cell should prefer (in order) same item ID, off-screen, create new
                        (activeCells.find { it.item?.let(rendererSet::id) == id }?.also {
                            // Same item ID: Data change should be animated here
                            it.onPullForPlacing(size, item, index, inProgress)
                        } ?: reuseableCells.popOrNull()?.also {
                            // If placing just offscreen, place without animation.
                            it.view.withoutAnimation { it.onPullForPlacing(size, item, index, inProgress) }
                            activeCells += it
                        } ?: MyCell<Any>().also {
                            // If creating a new cell, make sure we don't animate.
                            cells.withoutAnimation {
                                it.setup(rendererSet.renderer(item), size, item, index, inProgress)
                            }
                            activeCells += it
                        }).also { usedCells += it }
                    },
                    viewport = viewport
                )
            }
            runPlacer()

            // Check for attachment to top/bottom
            var firstCell: MyCell<*>? = null
            var lastCell: MyCell<*>? = null
            usedCells.forEach {
                if (it.index == data.range.first) firstCell = it
                if (it.index == data.range.last) lastCell = it
            }

            log?.log("ATTACHMENT: $firstCell / $lastCell")
            // Move the sentinel to create a good end for scrolling
            val sentinelSize = if (lastCell != null) {
                if (vertical)
                    lastCell!!.bottomNew + placer.scrollBeyond
                else
                    lastCell!!.rightNew + placer.scrollBeyond

            } else reallyBig
            log?.log("SENTINEL SIZE: $sentinelSize")

            // Pull everything in a direction seamlessly, without interrupting animations.
            fun offset(x: Double, y: Double) {
                log?.log("OFFSET: $x / $y")
                activeCells.forEach {
                    it.offset(x, y)
                }
                viewport = viewport.copy(left = viewport.left + x, top = viewport.top + y, right = viewport.right + x, bottom = viewport.bottom + y)
                queuedScrollOffset = x to y

                //dang it, we have to rerun the layout to ensure every space is properly populated.
                runPlacer()
                log?.log("OFFSET FOLLOW-UP PLACEMENT EXECUTED")
            }

            if (firstCell != null) {
                // Shift everyone to attach to the top, preventing scrolling away past there
                if(abs(placer.scrollBeyond - firstCell!!.topNew) > 1.0) {
                    log?.log("SHIFTING CELLS TO ATTACH TO TOP")
                    if (vertical) offset(0.0, -firstCell!!.topNew + placer.scrollBeyond)
                    else offset(-firstCell!!.leftNew + placer.scrollBeyond, 0.0)
                }

                // Obnoxiously, we need to layout *again* to ensure we've filled out the entire space in this scenario.

            } else if (usedCells.isNotEmpty()) {
                val sampleCell = usedCells.first()
                // Ensure there is enough space to scroll upwards
                val needsRecentering = if (vertical) sampleCell.topNew !in (reallyBig * 1 / 4)..(reallyBig * 3 / 4)
                else sampleCell.topNew !in (reallyBig * 1 / 4)..(reallyBig * 3 / 4)
                if (needsRecentering) {
                    if (vertical) offset(0.0, reallyBig * 0.5 - sampleCell.topNew)
                    else offset(reallyBig * 0.5 - sampleCell.leftNew, 0.0)
                }
            }

            // Dismiss the cells we don't need anymore
            val unusedCells = activeCells - usedCells
            unusedCells.forEach {
                if (rectOverlaps(
                        it.leftNew,
                        it.topNew,
                        it.rightNew,
                        it.bottomNew,
                        viewport.left,
                        viewport.top,
                        viewport.right,
                        viewport.bottom,
                    )
                ) {
                    it.animatedDismiss()
                } else {
                    it.instantDismiss()
                }
            }
            anchor = null

            val v = if (vertical) within.copy(height = sentinelSize) else within.copy(width = sentinelSize)
            lastMeasure = v
            return v
        }

        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            if(stahp) return
            if(anchor != null) measure(layout, inProgress, within)
            val didJump = queuedScrollOffset != null
            queuedScrollOffset?.let {
                log?.log("EXECUTING SCROLL JUMP ${scroll.viewport.state.getOrNull()} += ${it}")
                scroll.offset(it.first, it.second)
                queuedScrollOffset = null
            }
            if(within == Size.Zero) return
            val viewport = scroll.viewport.state.getOrNull() ?: return
            if (viewport.width == 0.0 || viewport.height == 0.0) return
            try {
                inLayout = true

                fun commitPosition(it: MyCell<*>) {
                    if (it.leftNew != it.leftOld ||
                        it.topNew != it.topOld ||
                        it.rightNew != it.rightOld ||
                        it.bottomNew != it.bottomOld
                    ) {
                        log?.log("CHANGING POSITION FOR ${it.index} from (${it.leftOld}, ${it.topOld}) to (${it.leftNew}, ${it.topNew})")
                    }
                    inProgress.place(it.view, it.leftNew, it.topNew, it.rightNew, it.bottomNew)
                    it.left = it.leftNew
                    it.top = it.topNew
                    it.right = it.rightNew
                    it.bottom = it.bottomNew
                    it.leftOld = it.leftNew
                    it.topOld = it.topNew
                    it.rightOld = it.rightNew
                    it.bottomOld = it.bottomNew
                }

                activeCells.forEach { commitPosition(it) }
            } finally {
                inLayout = false
            }
//            if(didJump) stahp = true
        }

    }

    init {
        cells.delegate = programmaticLayoutDelegate
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

private fun <T> MutableList<T>.popOrNull(): T? = if (!isEmpty()) removeAt(lastIndex) else null
private fun rectOverlaps(
    l1: Double,
    t1: Double,
    r1: Double,
    b1: Double,
    l2: Double,
    t2: Double,
    r2: Double,
    b2: Double,
): Boolean = l1 < r2 && r1 > l2 && t1 < b2 && b1 > t2