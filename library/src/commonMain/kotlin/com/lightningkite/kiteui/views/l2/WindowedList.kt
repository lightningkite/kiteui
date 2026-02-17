// by Claude
package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Minimal virtualized list using spacers + transform-based scroll anchoring.
 *
 * Structure: Frame > Scrolling > Col > TopSpacer + ContentContainer + BottomSpacer
 * On scroll: emergency settle if transform too large; queue normal settle.
 * On frame: incrementally add/remove items at content edges based on viewport proximity.
 * On settle: if spacer visible, adjust spacer sizes to zero out transform.
 *
 * by Claude
 *
 * Still needs testing on non-web platforms.
 */
@Untested
class WindowedList<T>(
    private val vertical: Boolean = true,
    var log: Log? = null,
    /**
     * Number of items to render immediately when data first arrives, before viewport
     * information is available. Enables SSR and avoids a blank first frame.
     * by Claude
     */
    var initialRenderCount: Int = 10,
    /**
     * Index to start the initial render from. Items above this index are lazily rendered
     * when the user scrolls up. The top spacer is sized to the estimated height of the
     * skipped items, so the scrollbar reflects the full list length.
     * by Claude
     */
    var initialRenderIndex: Int = 0
) {
    lateinit var outerFrame: RView; private set
    private lateinit var scroll: ScrollingBehaviors
    private lateinit var topSpacer: RView
    private lateinit var contentContainer: RView
    private lateinit var bottomSpacer: RView

    private lateinit var dataReactive: Reactive<List<T>>
    private lateinit var renderFn: ViewWriter.(Reactive<T>, Reactive<Int>) -> Unit

    private val defaultItemHeight = 80.0
    private var totalItemCount = 0
    private var currentTransform = 0.0
    private var inLayout = false
    /**
     * Suppresses the scroll listener across async boundaries (e.g., waiting for browser layout
     * after initial render at a non-zero index). Unlike [inLayout], this stays true until
     * explicitly cleared, surviving across event loop ticks.
     * by Claude
     */
    private var suppressScrollLayout = false
    private var settleJob: Job? = null
    private var followUpRemover: (() -> Unit)? = null
    private val heightCache = mutableMapOf<Int, Double>()

    private class RenderedItem<T>(
        val index: Int,
        val view: RView,
        val itemSignal: Signal<T>,
        val indexSignal: Signal<Int>,
        var measuredSize: Double
    )

    private val renderedItems = mutableListOf<RenderedItem<T>>()

    // --- Reactive visible index properties --- by Claude

    private val _firstVisibleIndex = Signal(0)
    private val _lastVisibleIndex = Signal(0)
    private val _centerIndex = Signal(0)

    /** Index of the first item whose bottom edge is past the viewport top. by Claude */
    val firstVisibleIndex: Reactive<Int> get() = _firstVisibleIndex
    /** Index of the last item whose top edge is before the viewport bottom. by Claude */
    val lastVisibleIndex: Reactive<Int> get() = _lastVisibleIndex
    /** Index of the item closest to the viewport center. by Claude */
    val centerIndex: Reactive<Int> get() = _centerIndex

    /**
     * Actual visual start of a rendered item in scroll-content coordinates.
     * Uses screenRectangle() (getBoundingClientRect on JS) so both rects are in the same
     * coordinate space. The difference gives viewport-relative position; adding viewport.top
     * converts to scroll-content coordinates.
     * by Claude
     */
    private val RenderedItem<T>.start: Double get() {
        val viewport = scroll.viewport.state.getOrNull() ?: run {
            log?.warn("WL3 start: viewport null for item $index")
            return 0.0
        }
        val outerRect = outerFrame.screenRectangle() ?: run {
            log?.warn("WL3 start: outerFrame.screenRectangle() null for item $index")
            return 0.0
        }
        val itemRect = view.screenRectangle() ?: run {
            log?.warn("WL3 start: view.screenRectangle() null for item $index")
            return 0.0
        }
        return if (vertical) viewport.top + (itemRect.top - outerRect.top)
        else viewport.left + (itemRect.left - outerRect.left)
    }

    /** Actual visual end of a rendered item in scroll-content coordinates. by Claude */
    private val RenderedItem<T>.end: Double get() {
        val viewport = scroll.viewport.state.getOrNull() ?: run {
            log?.warn("WL3 end: viewport null for item $index")
            return 0.0
        }
        val outerRect = outerFrame.screenRectangle() ?: run {
            log?.warn("WL3 end: outerFrame.screenRectangle() null for item $index")
            return 0.0
        }
        val itemRect = view.screenRectangle() ?: run {
            log?.warn("WL3 end: view.screenRectangle() null for item $index")
            return 0.0
        }
        return if (vertical) viewport.top + (itemRect.bottom - outerRect.top)
        else viewport.left + (itemRect.right - outerRect.left)
    }

    // --- Estimation helpers --- by Claude

    private fun getGap(): Double = contentContainer.gap?.px ?: contentContainer.theme.gap.px

    private fun estimatePositionOf(index: Int, gap: Double): Double {
        var pos = 0.0
        for (i in 0 until index) pos += (heightCache[i] ?: defaultItemHeight) + gap
        return pos
    }

    private fun estimateTotalHeight(gap: Double): Double {
        if (totalItemCount == 0) return 0.0
        var total = 0.0
        for (i in 0 until totalItemCount) {
            total += heightCache[i] ?: defaultItemHeight
            if (i < totalItemCount - 1) total += gap
        }
        return total
    }

    private fun findIndexAtPosition(position: Double, gap: Double): Int {
        var pos = 0.0
        for (i in 0 until totalItemCount) {
            val h = heightCache[i] ?: defaultItemHeight
            if (pos + h > position) return i
            pos += h + gap
        }
        return maxOf(0, totalItemCount - 1)
    }

    private fun renderedContentHeight(gap: Double): Double {
        if (renderedItems.isEmpty()) return 0.0
        return renderedItems.sumOf { it.measuredSize } + (renderedItems.size - 1).coerceAtLeast(0) * gap
    }

    /** Actual visual start of rendered content in scroll coordinates. by Claude */
    private fun contentVisualStart(): Double = renderedItems.firstOrNull()?.start ?: 0.0

    /** Actual visual end of rendered content in scroll coordinates. by Claude */
    private fun contentVisualEnd(): Double = renderedItems.lastOrNull()?.end ?: 0.0

    // --- Build --- by Claude

    fun ViewWriter.build(
        data: Reactive<List<T>>,
        render: ViewWriter.(Reactive<T>, Reactive<Int>) -> Unit
    ) {
        val wl = this@WindowedList
        wl.dataReactive = data
        wl.renderFn = render

        frame {
            padding = 0.px
            wl.outerFrame = this

            if (vertical) {
                expanding.scrolling(vertical = true, horizontal = false) {
                    wl.scroll = this
                    disableScrollAnchoring() // by Claude
                }.col {
                    padding = null
                    frame { wl.topSpacer = this; setSizeConstraints(height = 0.px) }
                    col { wl.contentContainer = this; disableTransformTransition() }
                    frame { wl.bottomSpacer = this; setSizeConstraints(height = 0.px) }
                    wl.setupListeners()
                }
            } else {
                expanding.scrolling(vertical = false, horizontal = true) {
                    wl.scroll = this
                    disableScrollAnchoring() // by Claude
                }.row {
                    padding = null
                    frame { wl.topSpacer = this; setSizeConstraints(width = 0.px) }
                    row { wl.contentContainer = this; disableTransformTransition() }
                    frame { wl.bottomSpacer = this; setSizeConstraints(width = 0.px) }
                    wl.setupListeners()
                }
            }
        }
    }

    // --- Event listeners --- by Claude

    private fun setupListeners() {
        var movingTimeoutRemover = {}
        contentContainer.onRemove(scroll.viewport.addListener {
            if (inLayout || suppressScrollLayout) return@addListener

            // On scroll: emergency settle if past list bounds
            settleJob?.cancel()
            settleJob = null
            val viewport = scroll.viewport.state.getOrNull()
            if (viewport != null && totalItemCount > 0) {
                val viewportStart = if (vertical) viewport.top else viewport.left
                val viewportEnd = if (vertical) viewport.bottom else viewport.right
                val first = renderedItems.find { it.index == 0 }
                val last = renderedItems.find { it.index == dataReactive.state.getOrNull()?.lastIndex }
                // Use tolerance to avoid triggering on iOS overscroll bounce. by Claude
                val emergency = (first != null && viewportStart < first.start - EMERGENCY_SETTLE_TOLERANCE) ||
                        (last != null && viewportEnd > last.end + EMERGENCY_SETTLE_TOLERANCE)
                if (emergency) {
                    log?.log("WL3 Emergency settle: viewportStart=$viewportStart viewportEnd=$viewportEnd firstStart=${first?.start} lastEnd=${last?.end}")
                    performSettle()
                    return@addListener
                }
            }

            // Queue settle
            movingTimeoutRemover()
            movingTimeoutRemover = afterTimeout(100) { scheduleSettle() }

            // Run layout once on this scroll event; schedule follow-up if more work needed
            runLayout()
            updateVisibleIndices()
        })

        // React to data changes
        contentContainer.reactive {
            val dataList = dataReactive()
            onDataChanged(dataList)
        }
    }

    /**
     * Run one pass of the incremental layout. If more items are needed to fill the buffer,
     * schedule a single follow-up frame. The follow-up can chain another if still needed.
     * Guarded to prevent infinite loops — stops after [maxLayoutIterations] consecutive passes.
     * by Claude
     */
    private var layoutIterationCount = 0
    private fun runLayout() {
        followUpRemover?.invoke()
        followUpRemover = null
        val needsMore = performLayoutPass()
        if (needsMore) {
            layoutIterationCount++
            if (layoutIterationCount >= MAX_LAYOUT_ITERATIONS) {
                log?.warn("WL3 runLayout: hit max iterations ($MAX_LAYOUT_ITERATIONS), stopping layout loop")
                layoutIterationCount = 0
                return
            }
            followUpRemover = afterTimeout(0) { runLayout() }
        } else {
            layoutIterationCount = 0
        }
    }

    companion object {
        /** Maximum consecutive layout passes before giving up, to prevent infinite loops. by Claude */
        const val MAX_LAYOUT_ITERATIONS = 50
        /** Tolerance in pixels for emergency settle to avoid triggering on iOS overscroll bounce. by Claude */
        const val EMERGENCY_SETTLE_TOLERANCE = 50.0
    }

    /**
     * One pass of the incremental layout algorithm. Adds at most one item at each boundary.
     * Returns true if the buffer is still not full and more work is needed.
     * by Claude
     */
    private fun performLayoutPass(): Boolean {
        val dataList = dataReactive.state.getOrNull() ?: return false
        if (dataList.isEmpty()) return false
        val viewport = scroll.viewport.state.getOrNull() ?: return false
        if (viewport.width == 0.0 || viewport.height == 0.0) return false

        inLayout = true
        try {
            val viewportStart = if (vertical) viewport.top else viewport.left
            val viewportEnd = if (vertical) viewport.bottom else viewport.right
            val viewportSize = viewportEnd - viewportStart
            val gap = getGap()
            val cStart = contentVisualStart()
            val cEnd = contentVisualEnd()

            // Far out of bounds: viewport has no meaningful overlap with rendered content
            if (renderedItems.isEmpty() ||
                viewportEnd < cStart - viewportSize ||
                viewportStart > cEnd + viewportSize
            ) {
                log?.log("WL3 Far out of bounds: viewport=$viewportStart..$viewportEnd content=$cStart..$cEnd")
                val approxIndex = findIndexAtPosition(max(0.0, viewportStart - viewportSize / 2), gap)
                clearAllItems()
                fillFrom(dataList, approxIndex, viewportSize * 2, gap)
                updateSpacers(gap)
                return false
            }

            var changed = false
            var needsMore = false

            log?.log("WL3 layoutPass: viewport=$viewportStart..$viewportEnd content=$cStart..$cEnd rendered=${renderedItems.firstOrNull()?.index}..${renderedItems.lastOrNull()?.index} transform=$currentTransform")

            // Near content boundary end: append one item
            val lastIdx = renderedItems.lastOrNull()?.index ?: -1
            if (lastIdx < totalItemCount - 1 && viewportEnd + viewportSize > contentVisualEnd()) {
                appendItem(dataList, lastIdx + 1)
                changed = true
                // Check if buffer is still not full
                if (lastIdx + 1 < totalItemCount - 1 && viewportEnd + viewportSize > contentVisualEnd()) {
                    needsMore = true
                }
            }

            // Near content boundary start: prepend one item
            val firstIdx = renderedItems.firstOrNull()?.index ?: 0
            if (firstIdx > 0 && viewportStart - viewportSize < contentVisualStart()) {
                prependItem(dataList, firstIdx - 1, gap)
                changed = true
                // Check if buffer is still not full
                if (firstIdx - 1 > 0 && viewportStart - viewportSize < contentVisualStart()) {
                    needsMore = true
                }
            }

            // Batch trim (runs every pass but is cheap when nothing to trim)
            batchTrim(viewportStart, viewportEnd, viewportSize, gap)

            // Only update the bottom spacer during normal layout.
            // The top spacer stays fixed — transform handles positioning.
            // Full spacer recalculation (including top) only happens during settle
            // and far-out-of-bounds reset. by Claude
            if (changed) updateBottomSpacer(gap)
            if (needsMore) log?.log("WL3 layoutPass: needsMore=true, iteration=$layoutIterationCount")
            return needsMore
        } finally {
            inLayout = false
        }
    }

    // --- Item management --- by Claude

    private fun fillFrom(dataList: List<T>, startIndex: Int, targetHeight: Double, gap: Double) {
        var height = 0.0
        var i = startIndex.coerceIn(0, maxOf(0, totalItemCount - 1))
        while (i < totalItemCount && height < targetHeight) {
            appendItem(dataList, i)
            height += (heightCache[i] ?: defaultItemHeight) + gap
            i++
        }
    }

    private fun appendItem(dataList: List<T>, index: Int) {
        if (index !in dataList.indices) return
        log?.log("appendItem: $index")
        val itemSignal = Signal(dataList[index])
        val indexSignal = Signal(index)
        val insertPos = contentContainer.children.size
        val writer = createItemWriter(insertPos)
        contentContainer.withoutAnimation { writer.renderFn(itemSignal, indexSignal) }
        val view = contentContainer.children.getOrNull(insertPos) ?: return
        val measured = measureView(view)
        heightCache[index] = measured
        renderedItems.add(RenderedItem(index, view, itemSignal, indexSignal, measured))
    }

    private fun prependItem(dataList: List<T>, index: Int, gap: Double) {
        if (index !in dataList.indices) return
        log?.log("prependItem: $index")
        val itemSignal = Signal(dataList[index])
        val indexSignal = Signal(index)
        val writer = createItemWriter(0)
        contentContainer.withoutAnimation { writer.renderFn(itemSignal, indexSignal) }
        val view = contentContainer.children.getOrNull(0) ?: return
        val measured = measureView(view)
        heightCache[index] = measured
        renderedItems.add(0, RenderedItem(index, view, itemSignal, indexSignal, measured))
        // Offset transform so existing content doesn't shift visually
        currentTransform -= measured + gap
        applyTransform()
    }

    /**
     * Batch trim: remove all items far from the viewport in one pass.
     * Re-measures each item's actual current size (via screenRectangle) before removing,
     * so the transform adjustment and height cache stay accurate even if items resized.
     * by Claude
     */
    private fun batchTrim(viewportStart: Double, viewportEnd: Double, viewportSize: Double, gap: Double): Boolean {
        val trimDistance = viewportSize * 3

        // Find how many to remove from the end
        var endTrimCount = 0
        for (i in renderedItems.indices.reversed()) {
            if (renderedItems.size - endTrimCount <= 1) break
            val itemStart = renderedItems[i].start
            if (itemStart > viewportEnd + trimDistance) {
                log?.log("WL3 batchTrim: end item ${renderedItems[i].index} start=$itemStart > threshold=${viewportEnd + trimDistance}")
                endTrimCount++
            }
            else break
        }

        // Find how many to remove from the start
        var startTrimCount = 0
        for (i in renderedItems.indices) {
            if (renderedItems.size - endTrimCount - startTrimCount <= 1) break
            val itemEnd = renderedItems[i].end
            if (itemEnd < viewportStart - trimDistance) {
                log?.log("WL3 batchTrim: start item ${renderedItems[i].index} end=$itemEnd < threshold=${viewportStart - trimDistance}")
                startTrimCount++
            }
            else break
        }

        if (startTrimCount == 0 && endTrimCount == 0) return false

        // Remove from end (no transform adjustment needed)
        repeat(endTrimCount) {
            val item = renderedItems.removeAt(renderedItems.lastIndex)
            // Re-measure actual current size before removing from DOM
            val actualSize = (item.end - item.start).let { if (it > 0) it else item.measuredSize }
            heightCache[item.index] = actualSize
            contentContainer.removeChild(item.view)
        }

        // Remove from start (single transform adjustment using actual current sizes)
        if (startTrimCount > 0) {
            var removedHeight = 0.0
            repeat(startTrimCount) {
                val item = renderedItems.removeAt(0)
                // Re-measure actual current size before removing from DOM
                val actualSize = (item.end - item.start).let { if (it > 0) it else item.measuredSize }
                heightCache[item.index] = actualSize
                removedHeight += actualSize + gap
                contentContainer.removeChild(item.view)
            }
            currentTransform += removedHeight
            applyTransform()
        }

        log?.log("WL3 Batch trim: removed $startTrimCount from start, $endTrimCount from end")
        return true
    }

    private fun clearAllItems() {
        for (item in renderedItems) heightCache[item.index] = item.measuredSize
        renderedItems.clear()
        contentContainer.clearChildren()
        currentTransform = 0.0
        applyTransform()
    }

    // --- Measurement --- by Claude

    private fun measureView(view: RView): Double {
        val rect = view.screenRectangle()
        if (rect == null) {
            log?.warn("WL3 measureView: screenRectangle() null, using defaultItemHeight=$defaultItemHeight")
            return defaultItemHeight
        }
        val size = if (vertical) rect.height else rect.width
        return if (size > 0) size else {
            log?.warn("WL3 measureView: size=$size <= 0, using defaultItemHeight=$defaultItemHeight")
            defaultItemHeight
        }
    }

    // --- Transform --- by Claude

    private fun applyTransform() {
        contentContainer.setTranslation(
            x = if (vertical) 0.0 else currentTransform,
            y = if (vertical) currentTransform else 0.0
        )
    }

    // --- Spacers --- by Claude

    private fun updateSpacers(gap: Double) {
        val topHeight = if (renderedItems.isEmpty()) 0.0
        else estimatePositionOf(renderedItems.first().index, gap)
        val bottomHeight = max(0.0, estimateTotalHeight(gap) - topHeight - renderedContentHeight(gap))
        if (vertical) {
            topSpacer.setSizeConstraints(height = max(0.0, topHeight).toInt().px)
            bottomSpacer.setSizeConstraints(height = max(0.0, bottomHeight).toInt().px)
        } else {
            topSpacer.setSizeConstraints(width = max(0.0, topHeight).toInt().px)
            bottomSpacer.setSizeConstraints(width = max(0.0, bottomHeight).toInt().px)
        }
    }

    /**
     * Updates only the bottom spacer, leaving the top spacer unchanged.
     * Used during normal incremental layout to avoid double-adjustment when prepending items
     * (prependItem already adjusts transform; changing the top spacer too would shift content twice).
     * by Claude
     */
    private fun updateBottomSpacer(gap: Double) {
        val topHeight = if (renderedItems.isEmpty()) 0.0
        else estimatePositionOf(renderedItems.first().index, gap)
        val bottomHeight = max(0.0, estimateTotalHeight(gap) - topHeight - renderedContentHeight(gap))
        if (vertical) {
            bottomSpacer.setSizeConstraints(height = max(0.0, bottomHeight).toInt().px)
        } else {
            bottomSpacer.setSizeConstraints(width = max(0.0, bottomHeight).toInt().px)
        }
    }

    // --- Settle --- by Claude

    private fun scheduleSettle() {
        settleJob?.cancel()
        settleJob = outerFrame.launch {
            delay(150)
            performSettle()
        }
    }

    /**
     * Zero out the transform by absorbing it into the top spacer.
     *
     * New top spacer = current top spacer + current transform. This is exact — no estimates
     * needed — because the transform accumulated from actual measured prepend/trim offsets.
     * When element 0 is rendered we know the top spacer must be 0.
     *
     * In the normal case (newTopHeight >= 0), no scroll adjustment is needed: the content's
     * visual position (spacer + transform) stays the same when we move the transform into
     * the spacer. If newTopHeight would go negative (clamped to 0), we adjust scroll to
     * compensate.
     * by Claude
     */
    private fun performSettle() {
        if (renderedItems.isEmpty()) return
        if (abs(currentTransform) < 1.0) return

        val viewport = scroll.viewport.state.getOrNull() ?: return
        val gap = getGap()

        log?.log("WL3 Settling: transform=$currentTransform")

        inLayout = true
        try {
            val firstIdx = renderedItems.first().index
            val lastIdx = renderedItems.last().index

            val currentTopHeight = topSpacer.screenRectangle()?.let {
                if (vertical) it.height else it.width
            } ?: 0.0

            // Derive new top spacer from actual accumulated offsets, not estimates.
            // Override to 0 when element 0 is rendered (we know exactly).
            val newTopHeight = if (firstIdx == 0) 0.0
                else max(0.0, currentTopHeight + currentTransform)

            // Scroll adjustment: only non-zero when clamping or overriding to 0.
            // Formula: (newTopHeight - currentTopHeight) - currentTransform
            // Normal case (newTopHeight = currentTopHeight + transform): scrollAdjust = 0
            val scrollAdjust = (newTopHeight - currentTopHeight) - currentTransform
            val currentScroll = if (vertical) viewport.top else viewport.left

            if (vertical) {
                topSpacer.setSizeConstraints(height = max(0.0, newTopHeight).toInt().px)
                if (abs(scrollAdjust) > 0.5) scroll.scrollTo(0.0, max(0.0, currentScroll + scrollAdjust), false)
            } else {
                topSpacer.setSizeConstraints(width = max(0.0, newTopHeight).toInt().px)
                if (abs(scrollAdjust) > 0.5) scroll.scrollTo(max(0.0, currentScroll + scrollAdjust), 0.0, false)
            }

            currentTransform = 0.0
            applyTransform()

            // Bottom spacer: estimate remaining items after last rendered.
            // This only affects scrollbar thumb position, so estimates are acceptable.
            val bottomHeight = if (lastIdx >= totalItemCount - 1) 0.0
            else {
                var h = 0.0
                for (i in (lastIdx + 1) until totalItemCount) {
                    if (i > lastIdx + 1) h += gap
                    h += heightCache[i] ?: defaultItemHeight
                }
                h
            }

            if (vertical) bottomSpacer.setSizeConstraints(height = max(0.0, bottomHeight).toInt().px)
            else bottomSpacer.setSizeConstraints(width = max(0.0, bottomHeight).toInt().px)

            log?.log("WL3 Settled: top=$newTopHeight bottom=$bottomHeight scrollAdjust=$scrollAdjust")
        } finally {
            inLayout = false
        }
    }

    // --- Scroll to index --- by Claude

    /**
     * Scrolls to make the item at [index] visible with the given [align]ment.
     *
     * If the item is already rendered, scrolls directly (animated or instant).
     * If not rendered and [animate] is true, first jumps so the item is barely on screen,
     * then finishes with an animated scroll to the desired alignment.
     * If not rendered and [animate] is false, jumps directly to the target position.
     * by Claude
     */
    fun scrollToIndex(index: Int, align: Align = Align.Start, animate: Boolean = true) {
        val dataList = dataReactive.state.getOrNull() ?: return
        if (dataList.isEmpty()) return
        val toIndex = index.coerceIn(0, dataList.lastIndex)
        val viewport = scroll.viewport.state.getOrNull() ?: return
        val viewportSize = if (vertical) viewport.height else viewport.width
        if (viewportSize <= 0) return

        log?.log("WL3 scrollToIndex: index=$toIndex align=$align animate=$animate")

        // Case 1: Item already rendered — just scroll to it
        renderedItems.find { it.index == toIndex }?.let { item ->
            scrollViewToItem(item, align, viewportSize, animate)
            return
        }

        // Case 2: Item not rendered
        val gap = getGap()
        if (animate && renderedItems.isNotEmpty()) {
            // Animated jump: position target barely on screen, then animate to final alignment.
            // Determine direction: is the target ahead of or behind the current view?
            val destinationAhead = toIndex > (renderedItems.lastOrNull()?.index ?: 0)
            val edgeAlign = if (destinationAhead) Align.End else Align.Start

            jumpRenderAround(dataList, toIndex, edgeAlign, viewportSize, gap)

            // After one frame, the item is rendered and laid out. Do the final animated scroll.
            afterTimeout(16) {
                renderedItems.find { it.index == toIndex }?.let { item ->
                    scrollViewToItem(item, align, viewportSize, true)
                } ?: run {
                    // Fallback: item still not rendered (shouldn't happen). Jump non-animated.
                    log?.warn("WL3 scrollToIndex: item $toIndex not rendered after jump, falling back")
                    jumpRenderAround(dataList, toIndex, align, viewportSize, gap)
                }
            }
        } else {
            // Non-animated jump: position directly at the desired alignment.
            jumpRenderAround(dataList, toIndex, align, viewportSize, gap)
        }
    }

    /**
     * Calculates the scroll target for an already-rendered item and scrolls to it.
     * by Claude
     */
    private fun scrollViewToItem(item: RenderedItem<T>, align: Align, viewportSize: Double, animate: Boolean) {
        val scrollTarget = when (align) {
            Align.Start -> item.start
            Align.End -> item.end - viewportSize
            else -> (item.start + item.end) / 2 - viewportSize / 2
        }
        log?.log("WL3 scrollToItem: index=${item.index} target=$scrollTarget animate=$animate")
        inLayout = true
        try {
            if (vertical) scroll.scrollTo(0.0, max(0.0, scrollTarget), animate)
            else scroll.scrollTo(max(0.0, scrollTarget), 0.0, animate)
        } finally {
            inLayout = false
        }
    }

    /**
     * Clears all rendered items and re-renders around [targetIndex], positioned so the
     * target item is at [align] within the viewport. Sets spacers and scroll position
     * accordingly.
     * by Claude
     */
    private fun jumpRenderAround(dataList: List<T>, targetIndex: Int, align: Align, viewportSize: Double, gap: Double) {
        log?.log("WL3 jumpRenderAround: target=$targetIndex align=$align")
        clearAllItems()

        // Render items before AND after the target using appendItem only (no prependItem),
        // so no transform accumulates. This keeps spacer + scroll position consistent.
        // by Claude
        val beforeCount = (viewportSize / (defaultItemHeight + gap)).toInt().coerceAtMost(targetIndex)
        val startIndex = (targetIndex - beforeCount).coerceAtLeast(0)
        val fillHeight = viewportSize * 3
        fillFrom(dataList, startIndex, fillHeight, gap)

        updateSpacers(gap)

        // Calculate scroll position so targetIndex is at the desired alignment.
        // Use estimated position since the item may not have a valid screenRectangle yet.
        val itemPos = estimatePositionOf(targetIndex, gap)
        val itemHeight = heightCache[targetIndex] ?: defaultItemHeight
        val scrollTarget = when (align) {
            Align.Start -> itemPos
            Align.End -> itemPos + itemHeight - viewportSize
            else -> itemPos + itemHeight / 2 - viewportSize / 2
        }

        suppressScrollLayout = true
        inLayout = true
        try {
            if (vertical) scroll.scrollTo(0.0, max(0.0, scrollTarget), false)
            else scroll.scrollTo(max(0.0, scrollTarget), 0.0, false)
        } finally {
            inLayout = false
        }
        suppressScrollLayout = false
    }

    // --- Visible index tracking --- by Claude

    /**
     * Updates the visible index reactive properties based on current viewport and rendered items.
     * Called after each layout pass.
     * by Claude
     */
    private fun updateVisibleIndices() {
        if (renderedItems.isEmpty()) return
        val viewport = scroll.viewport.state.getOrNull() ?: return
        val viewportStart = if (vertical) viewport.top else viewport.left
        val viewportEnd = if (vertical) viewport.bottom else viewport.right
        val viewportCenter = (viewportStart + viewportEnd) / 2

        var firstVisible = renderedItems.first().index
        var lastVisible = renderedItems.last().index
        var closestToCenter = renderedItems.first().index
        var closestCenterDist = Double.MAX_VALUE

        for (item in renderedItems) {
            val iStart = item.start
            val iEnd = item.end
            // First visible: first item whose bottom is past viewport top
            if (iEnd > viewportStart) {
                firstVisible = item.index
                // Find remaining
                for (j in renderedItems.indices) {
                    val jItem = renderedItems[j]
                    val jStart = jItem.start
                    val jEnd = jItem.end
                    // Last visible: last item whose top is before viewport bottom
                    if (jStart < viewportEnd) lastVisible = jItem.index
                    // Center: closest item center to viewport center
                    val itemCenter = (jStart + jEnd) / 2
                    val dist = abs(itemCenter - viewportCenter)
                    if (dist < closestCenterDist) {
                        closestCenterDist = dist
                        closestToCenter = jItem.index
                    }
                }
                break
            }
        }

        _firstVisibleIndex.value = firstVisible
        _lastVisibleIndex.value = lastVisible
        _centerIndex.value = closestToCenter
    }

    // --- Data changes --- by Claude

    private fun onDataChanged(dataList: List<T>) {
        val oldCount = totalItemCount
        totalItemCount = dataList.size
        log?.log("WL3 onDataChanged: $oldCount -> $totalItemCount items, rendered=${renderedItems.size} (${renderedItems.firstOrNull()?.index}..${renderedItems.lastOrNull()?.index})")

        if (totalItemCount == 0) {
            clearAllItems()
            updateSpacers(getGap())
            return
        }

        // Remove items beyond new list bounds
        var removedCount = 0
        while (renderedItems.isNotEmpty() && renderedItems.last().index >= totalItemCount) {
            val item = renderedItems.removeAt(renderedItems.lastIndex)
            contentContainer.removeChild(item.view)
            heightCache[item.index] = item.measuredSize
            removedCount++
        }
        if (removedCount > 0) log?.log("WL3 onDataChanged: trimmed $removedCount items beyond new bounds")

        // Update existing item signals
        for (item in renderedItems) {
            if (item.index in dataList.indices) item.itemSignal.value = dataList[item.index]
        }

        updateSpacers(getGap())

        // Initial fill if nothing rendered yet. Render initialRenderCount items starting
        // from initialRenderIndex, before viewport information arrives (important for SSR).
        // Items above initialRenderIndex are lazily prepended when the user scrolls up.
        // by Claude
        if (renderedItems.isEmpty()) {
            val startIdx = initialRenderIndex.coerceIn(0, dataList.lastIndex)
            val count = minOf(initialRenderCount, dataList.size - startIdx)
            if (count > 0) {
                log?.log("WL3 initial render: $count items starting at index $startIdx")
                for (i in startIdx until startIdx + count) appendItem(dataList, i)
                updateSpacers(getGap())
                if (startIdx > 0) {
                    // Scroll to the rendered content so the viewport aligns with it.
                    // The browser needs one frame to process the spacer height changes before
                    // scrollTo can reach the target position (otherwise scrollTop gets clamped
                    // to the old scrollHeight). We suppress the scroll listener during this
                    // window to prevent "far out of bounds" from resetting to index 0.
                    // by Claude
                    val scrollTarget = estimatePositionOf(startIdx, getGap())
                    log?.log("WL3 initial render: deferring scrollTo $scrollTarget for startIdx=$startIdx")
                    suppressScrollLayout = true
                    afterTimeout(0) {
                        log?.log("WL3 initial render: executing deferred scrollTo $scrollTarget")
                        inLayout = true
                        try {
                            if (vertical) scroll.scrollTo(0.0, scrollTarget, false)
                            else scroll.scrollTo(scrollTarget, 0.0, false)
                        } finally {
                            inLayout = false
                        }
                        suppressScrollLayout = false
                        // Don't call runLayout() here — the scrollTo will fire a scroll
                        // event with the correct viewport, and the scroll listener will
                        // handle layout. Calling runLayout() now would read a stale
                        // viewport (still at 0) and incorrectly prepend items.
                        // by Claude
                    }
                } else {
                    // Starting from index 0: viewport is already aligned, safe to run layout
                    runLayout()
                }
            } else {
                runLayout()
            }
        }
    }

    // --- ViewWriter helper --- by Claude

    private fun createItemWriter(insertIndex: Int): ViewWriter {
        return object : ViewWriter() {
            override val context: RContext get() = contentContainer.context
            override val representsView: RView? get() = contentContainer
            override val coroutineContext get() = contentContainer.coroutineContext
            override fun willAddChild(view: RView) { view.parent = contentContainer }
            override fun addChild(view: RView) { contentContainer.addChild(insertIndex, view) }
        }
    }
}

// --- Builder functions --- by Claude

/**
 * Still needs testing on platforms that aren't web.
 */
@Untested
fun <T> ViewWriter.windowedList(
    data: Reactive<List<T>>,
    setup: WindowedList<T>.() -> Unit = {},
    render: ViewWriter.(Reactive<T>, Reactive<Int>) -> Unit
): WindowedList<T> {
    val list = WindowedList<T>(vertical = true)
    setup(list)
    with(list) { build(data, render) }
    return list
}

/**
 * Still needs testing on platforms that aren't web.
 */
@Untested
fun <T> ViewWriter.horizontalWindowedList(
    data: Reactive<List<T>>,
    setup: WindowedList<T>.() -> Unit = {},
    render: ViewWriter.(Reactive<T>, Reactive<Int>) -> Unit
): WindowedList<T> {
    val list = WindowedList<T>(vertical = false)
    setup(list)
    with(list) { build(data, render) }
    return list
}
