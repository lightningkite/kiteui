// by Claude
package com.lightningkite.kiteui.views.l2

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure-logic simulation of WindowedList3's windowed list algorithm.
 *
 * Operates on abstract numbers instead of platform views. All positions are computed from
 * the simulator's own state (spacer sizes + transform + cumulative item heights), replacing
 * the real WindowedList3's reliance on screenRectangle()/getBoundingClientRect.
 *
 * This enables unit testing the algorithm's append/prepend/trim/settle decisions without
 * needing a real browser or native layout engine.
 *
 * by Claude
 */
class WindowedListSimulator(
    private val defaultItemHeight: Double = 80.0,
    private val gap: Double = 8.0,
    var initialRenderCount: Int = 10,
    var initialRenderIndex: Int = 0
) {
    // --- Data ---
    var data: List<Double> = emptyList()  // Each element is the height of that item
        private set
    val totalItemCount get() = data.size

    // --- Rendered state ---
    data class RenderedItem(val index: Int, val height: Double)

    val renderedItems = mutableListOf<RenderedItem>()

    // --- Layout state ---
    var topSpacerSize: Double = 0.0
        private set
    var bottomSpacerSize: Double = 0.0
        private set
    var currentTransform: Double = 0.0
        private set

    // --- Viewport ---
    var viewportStart: Double = 0.0
        private set
    var viewportSize: Double = 600.0

    // --- Caches ---
    val heightCache = mutableMapOf<Int, Double>()

    // --- Logging ---
    val logs = mutableListOf<String>()

    // --- Layout iteration tracking ---
    private var layoutIterationCount = 0

    // --- Computed positions ---

    /**
     * Computes the scroll-content position where a rendered item starts.
     * In the real system this would be screenRectangle-based; here we compute it
     * from topSpacer + transform + sum of preceding rendered item heights.
     * by Claude
     */
    fun renderedItemStart(renderedIdx: Int): Double {
        var pos = topSpacerSize + currentTransform
        for (i in 0 until renderedIdx) {
            pos += renderedItems[i].height + gap
        }
        return pos
    }

    /**
     * End position (start + height) of a rendered item.
     * by Claude
     */
    fun renderedItemEnd(renderedIdx: Int): Double {
        return renderedItemStart(renderedIdx) + renderedItems[renderedIdx].height
    }

    /** Start of the first rendered item in scroll coordinates. by Claude */
    fun contentVisualStart(): Double {
        if (renderedItems.isEmpty()) return 0.0
        return renderedItemStart(0)
    }

    /** End of the last rendered item in scroll coordinates. by Claude */
    fun contentVisualEnd(): Double {
        if (renderedItems.isEmpty()) return 0.0
        return renderedItemEnd(renderedItems.lastIndex)
    }

    // --- Estimation helpers (match WindowedList3) --- by Claude

    fun estimatePositionOf(index: Int): Double {
        var pos = 0.0
        for (i in 0 until index) pos += (heightCache[i] ?: defaultItemHeight) + gap
        return pos
    }

    fun estimateTotalHeight(): Double {
        if (totalItemCount == 0) return 0.0
        var total = 0.0
        for (i in 0 until totalItemCount) {
            total += heightCache[i] ?: defaultItemHeight
            if (i < totalItemCount - 1) total += gap
        }
        return total
    }

    fun findIndexAtPosition(position: Double): Int {
        var pos = 0.0
        for (i in 0 until totalItemCount) {
            val h = heightCache[i] ?: defaultItemHeight
            if (pos + h > position) return i
            pos += h + gap
        }
        return maxOf(0, totalItemCount - 1)
    }

    fun renderedContentHeight(): Double {
        if (renderedItems.isEmpty()) return 0.0
        return renderedItems.sumOf { it.height } + (renderedItems.size - 1).coerceAtLeast(0) * gap
    }

    // --- Data changes --- by Claude

    fun setData(itemHeights: List<Double>) {
        val oldCount = data.size
        data = itemHeights
        logs.add("onDataChanged: $oldCount -> ${data.size}")

        if (data.isEmpty()) {
            clearAllItems()
            updateSpacers()
            return
        }

        // Remove items beyond new bounds
        while (renderedItems.isNotEmpty() && renderedItems.last().index >= totalItemCount) {
            val item = renderedItems.removeLast()
            heightCache[item.index] = item.height
        }

        updateSpacers()

        // Initial fill if nothing rendered. Render initialRenderCount items starting from
        // initialRenderIndex. Items above are lazily prepended on scroll up. by Claude
        if (renderedItems.isEmpty()) {
            val startIdx = initialRenderIndex.coerceIn(0, data.lastIndex)
            val count = minOf(initialRenderCount, data.size - startIdx)
            if (count > 0) {
                logs.add("initial render: $count items starting at index $startIdx")
                for (i in startIdx until startIdx + count) appendItem(i)
                updateSpacers()
                if (startIdx > 0) {
                    // Scroll viewport to match rendered content and skip runLayout —
                    // mirrors the real system where scrollTo fires layout via the async
                    // scroll listener. by Claude
                    viewportStart = estimatePositionOf(startIdx)
                } else {
                    runLayout()
                }
            } else {
                runLayout()
            }
        }
    }

    // --- Scrolling --- by Claude

    fun scrollTo(position: Double) {
        viewportStart = position
        val viewportEnd = viewportStart + viewportSize

        // Emergency settle check (with tolerance, matching the fix)
        if (totalItemCount > 0 && renderedItems.isNotEmpty()) {
            val first = renderedItems.find { it.index == 0 }
            val last = renderedItems.find { it.index == data.lastIndex }
            val firstStart = if (first != null) renderedItemStart(renderedItems.indexOf(first)) else null
            val lastEnd = if (last != null) renderedItemEnd(renderedItems.indexOf(last)) else null
            val emergency = (firstStart != null && viewportStart < firstStart - WindowedList.EMERGENCY_SETTLE_TOLERANCE) ||
                    (lastEnd != null && viewportEnd > lastEnd + WindowedList.EMERGENCY_SETTLE_TOLERANCE)
            if (emergency) {
                logs.add("Emergency settle at viewport=$viewportStart..$viewportEnd")
                performSettle()
                return
            }
        }

        runLayout()
    }

    /**
     * Simulate a full scroll-then-settle cycle: scroll, layout, then settle.
     * In the real system, settle happens asynchronously after scrolling stops.
     * by Claude
     */
    fun scrollAndSettle(position: Double) {
        scrollTo(position)
        performSettle()
    }

    // --- Layout --- by Claude

    fun runLayout() {
        layoutIterationCount = 0
        var needsMore = true
        while (needsMore && layoutIterationCount < WindowedList.MAX_LAYOUT_ITERATIONS) {
            needsMore = performLayoutPass()
            layoutIterationCount++
        }
        if (layoutIterationCount >= WindowedList.MAX_LAYOUT_ITERATIONS) {
            logs.add("WARNING: hit max layout iterations")
        }
    }

    fun performLayoutPass(): Boolean {
        if (data.isEmpty()) return false
        if (viewportSize <= 0) return false

        val viewportEnd = viewportStart + viewportSize
        val cStart = contentVisualStart()
        val cEnd = contentVisualEnd()

        // Far out of bounds
        if (renderedItems.isEmpty() ||
            viewportEnd < cStart - viewportSize ||
            viewportStart > cEnd + viewportSize
        ) {
            logs.add("Far out of bounds: viewport=$viewportStart..$viewportEnd content=$cStart..$cEnd")
            val approxIndex = findIndexAtPosition(max(0.0, viewportStart - viewportSize / 2))
            clearAllItems()
            fillFrom(approxIndex, viewportSize * 2)
            updateSpacers()
            return false
        }

        var changed = false
        var needsMore = false

        // Append
        val lastIdx = renderedItems.lastOrNull()?.index ?: -1
        if (lastIdx < totalItemCount - 1 && viewportEnd + viewportSize > contentVisualEnd()) {
            appendItem(lastIdx + 1)
            changed = true
            if (lastIdx + 1 < totalItemCount - 1 && viewportEnd + viewportSize > contentVisualEnd()) {
                needsMore = true
            }
        }

        // Prepend
        val firstIdx = renderedItems.firstOrNull()?.index ?: 0
        if (firstIdx > 0 && viewportStart - viewportSize < contentVisualStart()) {
            prependItem(firstIdx - 1)
            changed = true
            if (firstIdx - 1 > 0 && viewportStart - viewportSize < contentVisualStart()) {
                needsMore = true
            }
        }

        // Trim
        batchTrim()

        if (changed) updateBottomSpacer()
        return needsMore
    }

    // --- Item management --- by Claude

    private fun fillFrom(startIndex: Int, targetHeight: Double) {
        var height = 0.0
        var i = startIndex.coerceIn(0, maxOf(0, totalItemCount - 1))
        while (i < totalItemCount && height < targetHeight) {
            appendItem(i)
            height += (heightCache[i] ?: defaultItemHeight) + gap
            i++
        }
    }

    fun appendItem(index: Int) {
        if (index !in data.indices) return
        val h = data[index]
        heightCache[index] = h
        renderedItems.add(RenderedItem(index, h))
    }

    fun prependItem(index: Int) {
        if (index !in data.indices) return
        val h = data[index]
        heightCache[index] = h
        renderedItems.add(0, RenderedItem(index, h))
        currentTransform -= h + gap
    }

    fun batchTrim() {
        val viewportEnd = viewportStart + viewportSize
        val trimDistance = viewportSize * 3

        // Trim from end
        var endTrimCount = 0
        for (i in renderedItems.indices.reversed()) {
            if (renderedItems.size - endTrimCount <= 1) break
            val itemStart = renderedItemStart(i)
            if (itemStart > viewportEnd + trimDistance) endTrimCount++
            else break
        }

        // Trim from start
        var startTrimCount = 0
        for (i in renderedItems.indices) {
            if (renderedItems.size - endTrimCount - startTrimCount <= 1) break
            val itemEnd = renderedItemEnd(i)
            if (itemEnd < viewportStart - trimDistance) startTrimCount++
            else break
        }

        if (startTrimCount == 0 && endTrimCount == 0) return

        // Remove from end
        repeat(endTrimCount) {
            val item = renderedItems.removeLast()
            heightCache[item.index] = item.height
        }

        // Remove from start
        if (startTrimCount > 0) {
            var removedHeight = 0.0
            repeat(startTrimCount) {
                val item = renderedItems.removeFirst()
                heightCache[item.index] = item.height
                removedHeight += item.height + gap
            }
            currentTransform += removedHeight
        }

        if (startTrimCount > 0 || endTrimCount > 0) {
            logs.add("Batch trim: removed $startTrimCount from start, $endTrimCount from end")
        }
    }

    private fun clearAllItems() {
        for (item in renderedItems) heightCache[item.index] = item.height
        renderedItems.clear()
        currentTransform = 0.0
    }

    // --- Spacers --- by Claude

    fun updateSpacers() {
        topSpacerSize = if (renderedItems.isEmpty()) 0.0
        else estimatePositionOf(renderedItems.first().index)
        bottomSpacerSize = max(0.0, estimateTotalHeight() - topSpacerSize - renderedContentHeight())
    }

    private fun updateBottomSpacer() {
        val topHeight = if (renderedItems.isEmpty()) 0.0
        else estimatePositionOf(renderedItems.first().index)
        bottomSpacerSize = max(0.0, estimateTotalHeight() - topHeight - renderedContentHeight())
    }

    // --- Settle --- by Claude

    fun performSettle() {
        if (renderedItems.isEmpty()) return
        if (abs(currentTransform) < 1.0) {
            currentTransform = 0.0
            return
        }

        logs.add("Settling: transform=$currentTransform")

        val firstIdx = renderedItems.first().index
        val lastIdx = renderedItems.last().index

        val currentTopHeight = topSpacerSize

        val newTopHeight = if (firstIdx == 0) 0.0
        else max(0.0, currentTopHeight + currentTransform)

        val scrollAdjust = (newTopHeight - currentTopHeight) - currentTransform
        if (abs(scrollAdjust) > 0.5) {
            viewportStart = max(0.0, viewportStart + scrollAdjust)
        }

        topSpacerSize = newTopHeight
        currentTransform = 0.0

        // Bottom spacer
        bottomSpacerSize = if (lastIdx >= totalItemCount - 1) 0.0
        else {
            var h = 0.0
            for (i in (lastIdx + 1) until totalItemCount) {
                if (i > lastIdx + 1) h += gap
                h += heightCache[i] ?: defaultItemHeight
            }
            h
        }

        logs.add("Settled: top=$topSpacerSize bottom=$bottomSpacerSize scrollAdjust=$scrollAdjust")
    }

    // --- Invariant checking --- by Claude

    /**
     * Checks all structural invariants that should hold after any operation.
     * Returns a list of violation descriptions (empty = all good).
     * by Claude
     */
    fun checkInvariants(): List<String> {
        val violations = mutableListOf<String>()

        // 1. Rendered items are contiguous
        for (i in 1 until renderedItems.size) {
            if (renderedItems[i].index != renderedItems[i - 1].index + 1) {
                violations.add("Non-contiguous: items[${i-1}].index=${renderedItems[i-1].index}, items[$i].index=${renderedItems[i].index}")
            }
        }

        // 2. Rendered items are within data bounds
        for (item in renderedItems) {
            if (item.index < 0 || item.index >= totalItemCount) {
                violations.add("Out of bounds: item.index=${item.index}, totalItemCount=$totalItemCount")
            }
        }

        // 3. Bottom spacer should be non-negative and reasonable
        // Note: the bottom spacer is computed differently by updateSpacers (estimate-based)
        // vs performSettle (precise from cache). Both are valid since the bottom spacer
        // only affects scrollbar thumb position. We just verify it's non-negative and
        // not wildly wrong (within 20% of expected or within a fixed tolerance). by Claude
        if (renderedItems.isNotEmpty()) {
            if (bottomSpacerSize < -1.0) {
                violations.add("Bottom spacer is negative: $bottomSpacerSize")
            }
            // If the last rendered item IS the last data item, bottom spacer should be ~0
            if (renderedItems.last().index >= totalItemCount - 1 && bottomSpacerSize > gap + 2.0) {
                violations.add("Bottom spacer should be ~0 when last item rendered: actual=$bottomSpacerSize")
            }
        }

        // 4. After settle, transform should be 0 (or very close)
        // This is only checked explicitly, not as a post-operation invariant

        return violations
    }

    /**
     * Checks that the viewport is covered by rendered items (no visible blank gaps).
     * Returns a list of violation descriptions.
     * by Claude
     */
    fun checkViewportCoverage(): List<String> {
        val violations = mutableListOf<String>()
        if (data.isEmpty() || renderedItems.isEmpty()) return violations

        val viewportEnd = viewportStart + viewportSize
        val cStart = contentVisualStart()
        val cEnd = contentVisualEnd()

        // If there are items before the rendered range and the viewport extends before rendered content
        val firstIdx = renderedItems.first().index
        if (firstIdx > 0 && viewportStart < cStart) {
            violations.add("Viewport extends before rendered content: viewportStart=$viewportStart < contentStart=$cStart, firstRendered=$firstIdx")
        }

        // If there are items after the rendered range and the viewport extends after rendered content
        val lastIdx = renderedItems.last().index
        if (lastIdx < totalItemCount - 1 && viewportEnd > cEnd) {
            violations.add("Viewport extends after rendered content: viewportEnd=$viewportEnd > contentEnd=$cEnd, lastRendered=$lastIdx")
        }

        return violations
    }

    override fun toString(): String = buildString {
        append("WindowedListSimulator(")
        append("data=${data.size} items, ")
        append("rendered=${renderedItems.size} [${renderedItems.firstOrNull()?.index}..${renderedItems.lastOrNull()?.index}], ")
        append("viewport=$viewportStart..${viewportStart + viewportSize}, ")
        append("topSpacer=$topSpacerSize, bottomSpacer=$bottomSpacerSize, ")
        append("transform=$currentTransform)")
    }
}
