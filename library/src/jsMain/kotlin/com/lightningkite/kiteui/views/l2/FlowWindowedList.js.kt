@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.math.abs
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * The web's [WindowedList].
 *
 * The cells are ordinary flex children, so the browser measures and positions them; nothing here
 * computes a layout.  A spacer above and one below stand in for the items not rendered, which keeps
 * the native scrollbar spanning the whole list.  Cells are reused as the window moves.
 *
 * To assistive technology this is a list whose items report their position among all the items,
 * rendered or not.  The rendered cells are kept in item order in the DOM, since that is the order
 * screen readers read and the Tab key follows.
 */
@ExperimentalKiteUi
internal class FlowWindowedList(context: ElementContext) : NativeLinearLayoutElement(context), WindowedList {
    private val scroll: ScrollingBehaviors = ScrollingBehaviorImpl(this, horizontal = false, vertical = true)

    private val _firstIndex = Signal(0)
    private val _lastIndex = Signal(0)
    override val firstIndex: Reactive<Int> get() = _firstIndex
    override val lastIndex: Reactive<Int> get() = _lastIndex

    /** How far past the viewport, in pixels, cells stay rendered so a fast scroll doesn't outrun them. */
    private val overscan = 600.0

    private val topSpacer: Frame
    private val bottomSpacer: Frame

    init {
        native.tag = "div"
        native.style.display = "flex"
        native.style.flexDirection = "column"
        native.setAttribute("role", "list")
        // Lets the keyboard scroll a list whose cells hold nothing focusable, and gives focus somewhere
        // to go when the cell holding it is reused.
        native.attributes.tabIndex = 0
        // Safari has no scroll anchoring, so [holdAnchor] does that job the same way in every browser.
        native.setStyleProperty("overflow-anchor", "none")
        unpadded.frame {
            this@FlowWindowedList.topSpacer = this
            native.setAttribute("aria-hidden", "true")
            native.setStyleProperty("flex-shrink", "0")
            // Cancels the flex gap between the spacer and the first cell, so an empty spacer leaves no blank band.
            native.setStyleProperty("margin-bottom", "calc(-1 * var(--spacing, 0px))")
        }
        unpadded.frame {
            this@FlowWindowedList.bottomSpacer = this
            native.setAttribute("aria-hidden", "true")
            native.setStyleProperty("flex-shrink", "0")
            native.setStyleProperty("margin-top", "calc(-1 * var(--spacing, 0px))")
        }
        native.onElement {
            it.addEventListener("scroll", {
                scrolledSinceUpdate = true
                invalidate()
            })
        }
    }

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        onRemove(native.resizeObserver().addListener { invalidate() })
    }

    private var items: List<Any?> = emptyList()
    private var rendererSet: RecyclerViewRendererSet<Any?, Any?> = RecyclerViewRendererSet.Empty
    private fun idOf(item: Any?): Any? = rendererSet.id(item)
    private var childrenSet = false

    override fun <T, ID> children(items: Reactive<List<T>>, id: (T) -> ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit =
        setChildren(items, RecyclerViewRendererSet.single(id, render))

    override fun <T, ID> childrenMultipleTypes(items: Reactive<List<T>>, id: (T) -> ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit =
        setChildren(items, RecyclerViewRendererSet.multi(id, renderers))

    private fun <T> setChildren(items: Reactive<List<T>>, rendererSet: RecyclerViewRendererSet<T, *>) {
        if (childrenSet) throw IllegalStateException("A WindowedList's children can only be set once.")
        childrenSet = true
        @Suppress("UNCHECKED_CAST")
        this.rendererSet = rendererSet as RecyclerViewRendererSet<Any?, Any?>
        reactive {
            val newItems = items()
            this@FlowWindowedList.items = newItems
            extents = DoubleArray(newItems.size) { measuredExtentById[idOf(newItems[it])] ?: Double.NaN }
            shownMatchesItems = false
            invalidate()
        }
    }

    private inner class Cell(var item: Any?) {
        val data = RawReactive<Any?>().also { it.state = ReactiveState(item) }
        /** What built this cell.  It can only ever show items that pick the same one. */
        val renderer = rendererSet.renderer(item)
        private val indexSignal = Signal(-1)
        val index: Int get() = indexSignal.value
        var setSize = -1

        // The list item is a wrapper, so that what the caller renders keeps its own role (a link, a button).
        val view = object : NativeContainerElement(context) {
            init {
                native.tag = "div"
                native.style.display = "flex"
                native.setAttribute("role", "listitem")
                native.setStyleProperty("flex-shrink", "0")
            }

            override fun nativeAddChild(index: Int, element: Element) {
                super.nativeAddChild(index, element)
                element.underlyingNativeElement.native.style.flexGrow = "1"
            }
        }
        val dom: HTMLElement get() = view.native.element as HTMLElement

        init {
            // Lands after the bottom spacer, where cells not in the window are kept.
            this@FlowWindowedList.withoutAnimation { this@FlowWindowedList.write(view) { renderer.render(this, data, indexSignal) } }
            // A cell that changes height on its own (streamed text, a loaded image) moves everything after it.
            view.onRemove(view.native.resizeObserver().addListener {
                holdAnchor()
                invalidate()
            })
        }

        fun show(index: Int, item: Any?) {
            if (this.item != item) {
                this.item = item
                data.state = ReactiveState(item)
            }
            if (this.index != index) {
                indexSignal.value = index
                view.native.setAttribute("aria-posinset", (index + 1).toString())
            }
            if (setSize != items.size) {
                setSize = items.size
                view.native.setAttribute("aria-setsize", setSize.toString())
            }
            if (!view.shown) view.shown = true
        }
    }

    /** The cells on screen, in index order with no gaps. */
    private var shownCells = ArrayList<Cell>()
    private val hiddenCells = ArrayList<Cell>()

    /** False once [items] has changed under the [shownCells], until they are assigned again. */
    private var shownMatchesItems = true

    /** Each item's height plus the gap after it, or NaN if it has never been rendered. */
    private var extents = DoubleArray(0)
    private val measuredExtentById = HashMap<Any?, Double>()
    private var estimatedExtent = 80.0
    private fun extent(index: Int): Double = extents[index].let { if (it.isNaN()) estimatedExtent else it }

    private var topSpacerHeight = 0.0
    private var bottomSpacerHeight = 0.0

    private class ScrollTarget(val index: Int, val align: Align, val animate: Boolean)
    private var scrollTarget: ScrollTarget? = null

    override fun scrollToIndex(index: Int, align: Align, animate: Boolean) {
        if (index !in items.indices) throw IndexOutOfBoundsException("Index $index is not in ${items.indices}")
        scrollTarget = ScrollTarget(index, align, animate)
        invalidate()
    }

    private var scrolledSinceUpdate = false

    /**
     * The list was last scrolled to its end, so it stays there as items arrive or cells grow.
     * Only a scroll changes this: content growing under a list at its end must not unpin it.
     */
    private var pinnedToEnd = false

    /** The first cell in view and how far below the viewport's top it sat, as of the last [update]. */
    private var anchorCell: Cell? = null
    private var anchorOffset = 0.0

    /**
     * Keeps what the user is looking at in place when cells above it turn out taller or shorter than they were.
     * Run from the resize observer, which is after layout and before paint, so the shift is never shown.
     */
    private fun holdAnchor() {
        val scroller = native.element as? HTMLElement ?: return
        if (pinnedToEnd) {
            scroller.scrollTop = scroller.scrollHeight.toDouble()
            return
        }
        val cell = anchorCell ?: return
        val moved = cell.dom.getBoundingClientRect().top - scroller.getBoundingClientRect().top - anchorOffset
        if (abs(moved) >= 0.5) scroller.scrollTop += moved
    }

    private var frameRequested = false
    private fun invalidate() {
        if (frameRequested) return
        frameRequested = true
        // In a frame, after its scroll events and before its paint, the previous frame's layout is
        // still valid, so the reads in update() cost nothing.
        window.requestAnimationFrame {
            frameRequested = false
            update()
        }
    }

    private fun update() {
        val scroller = native.element as? HTMLElement ?: return
        val viewportHeight = scroller.clientHeight.toDouble()
        // Not displayed.  The resize observer invalidates again when it is.
        if (viewportHeight == 0.0) return
        var viewportTop = scroller.scrollTop
        val items = items
        val gap = window.getComputedStyle(scroller).getPropertyValue("row-gap").removeSuffix("px").toDoubleOrNull() ?: 0.0

        if (scrolledSinceUpdate) {
            scrolledSinceUpdate = false
            pinnedToEnd = viewportTop > 0.0 && viewportTop + viewportHeight >= scroller.scrollHeight - 2
        }
        if (shownMatchesItems) {
            recordShownExtents(gap)
            val scrollerTop = scroller.getBoundingClientRect().top
            anchorCell = shownCells.firstOrNull { it.dom.getBoundingClientRect().bottom > scrollerTop }
            anchorOffset = anchorCell?.let { it.dom.getBoundingClientRect().top - scrollerTop } ?: 0.0
        }

        // Where to put the scroll position once this pass has rendered what belongs there.
        var jumpTo: Double? = null
        val target = scrollTarget?.takeIf { it.index in items.indices }
        if (target != null) {
            val targetCell = shownCells.takeIf { shownMatchesItems }?.firstOrNull { it.index == target.index }
            if (targetCell != null) {
                // It's laid out, so its real position is known.
                val cellBounds = targetCell.dom.getBoundingClientRect()
                val scrollerBounds = scroller.getBoundingClientRect()
                val alignedTop = viewportTop + when (target.align) {
                    Align.Start -> cellBounds.top - scrollerBounds.top
                    Align.End -> cellBounds.bottom - scrollerBounds.bottom
                    else -> (cellBounds.top + cellBounds.bottom) / 2 - (scrollerBounds.top + scrollerBounds.bottom) / 2
                }
                scrollTarget = null
                anchorCell = null
                scroll.scrollTo(0.0, alignedTop, target.animate)
            } else {
                // It isn't rendered, so its position is a guess.  Go there, render, then align next frame.
                var aboveTarget = 0.0
                for (index in 0 until target.index) aboveTarget += extent(index)
                val guess = when (target.align) {
                    Align.Start -> aboveTarget
                    Align.End -> aboveTarget + extent(target.index) - viewportHeight
                    else -> aboveTarget + extent(target.index) / 2 - viewportHeight / 2
                }.coerceAtLeast(0.0)
                viewportTop = guess
                jumpTo = guess
            }
        } else if (pinnedToEnd) {
            var total = 0.0
            for (index in items.indices) total += extent(index)
            viewportTop = (total - viewportHeight).coerceAtLeast(0.0)
            // Past the end, which the browser clamps to the end as it really is after layout.
            jumpTo = total
        }

        var first = 0
        var aboveFirst = 0.0
        while (first < items.lastIndex && aboveFirst + extent(first) <= viewportTop - overscan) {
            aboveFirst += extent(first)
            first++
        }
        var end = first
        var aboveEnd = aboveFirst
        while (end < items.size && aboveEnd < viewportTop + viewportHeight + overscan) {
            aboveEnd += extent(end)
            end++
        }
        var belowEnd = 0.0
        for (index in end until items.size) belowEnd += extent(index)
        // Every extent includes the gap after its item, and nothing follows the last item.
        belowEnd = (belowEnd - gap).coerceAtLeast(0.0)

        if (aboveFirst != topSpacerHeight) {
            topSpacerHeight = aboveFirst
            topSpacer.native.setStyleProperty("height", "${aboveFirst}px")
        }
        if (belowEnd != bottomSpacerHeight) {
            bottomSpacerHeight = belowEnd
            bottomSpacer.native.setStyleProperty("height", "${belowEnd}px")
        }

        val windowChanged = !shownMatchesItems ||
                shownCells.size != end - first ||
                (shownCells.isNotEmpty() && shownCells.first().index != first)
        if (windowChanged) {
            showWindow(scroller, first, end)
            // The new cells were placed by estimate.  Next frame they can be measured, which may show
            // the window needs to reach further.
            invalidate()
        }
        // Still waiting to align on the target, which this pass has only just rendered.
        if (scrollTarget != null) invalidate()

        if (jumpTo != null) {
            // The view is moving on purpose, so there is nothing to hold in place.
            anchorCell = null
            scroller.scrollTop = jumpTo
        }
    }

    private fun showWindow(scroller: HTMLElement, first: Int, end: Int) {
        // A cell already showing an item keeps it, so that item's bindings don't rerun.
        val reusable = HashMap<Any?, Cell>()
        for (cell in shownCells) reusable[idOf(cell.item)] = cell
        val assigned = arrayOfNulls<Cell>(end - first)
        for (index in first until end) {
            val item = items[index]
            val showingIt = reusable[idOf(item)] ?: continue
            if (showingIt.renderer !== rendererSet.renderer(item)) continue
            reusable.remove(idOf(item))
            assigned[index - first] = showingIt
        }

        val spare = ArrayList<Cell>(hiddenCells)
        hiddenCells.clear()
        for (cell in reusable.values) {
            // Focus left on a reused cell would silently be on a different item.
            if (cell.dom.contains(document.activeElement)) scroller.focus()
            if (cell === anchorCell) anchorCell = null
            // Out of the sequence, so the cells that stay never have to move around it.
            scroller.appendChild(cell.dom)
            spare.add(cell)
        }

        val newShown = ArrayList<Cell>(end - first)
        var previous = topSpacer.native.element!!
        for (index in first until end) {
            val item = items[index]
            val renderer = rendererSet.renderer(item)
            val cell = assigned[index - first]
                ?: spare.indexOfLast { it.renderer === renderer }.takeIf { it != -1 }?.let { spare.removeAt(it) }
                ?: Cell(item)
            cell.show(index, item)
            if (cell.dom.previousElementSibling !== previous) scroller.insertBefore(cell.dom, previous.nextSibling)
            previous = cell.dom
            newShown.add(cell)
        }
        for (cell in spare) {
            if (cell.view.shown) cell.view.shown = false
            hiddenCells.add(cell)
        }
        shownCells = newShown
        shownMatchesItems = true
        _firstIndex.value = first
        _lastIndex.value = (end - 1).coerceAtLeast(0)
    }

    private fun recordShownExtents(gap: Double) {
        var total = 0.0
        for (cell in shownCells) {
            val measured = cell.dom.getBoundingClientRect().height + gap
            extents[cell.index] = measured
            measuredExtentById[idOf(cell.item)] = measured
            total += measured
        }
        // Changing the estimate resizes the top spacer, which would shift what's on screen; at the
        // top of the list that spacer is empty, so it's safe there.
        if (shownCells.isNotEmpty() && shownCells.first().index == 0) estimatedExtent = total / shownCells.size
    }
}

@ExperimentalKiteUi
internal actual fun platformWindowedList(context: ElementContext): WindowedList = FlowWindowedList(context)
