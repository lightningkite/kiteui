// by Claude
package com.lightningkite.kiteui.views.l2

import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for the windowed list algorithm via [WindowedListSimulator].
 *
 * Tests verify the core invariants:
 * - Rendered items are contiguous (no index gaps)
 * - Rendered items are within data bounds
 * - Spacer sizes are consistent with estimated total height
 * - Viewport is covered by rendered items (no blank gaps visible)
 * - After settle, transform is 0
 *
 * by Claude
 */
class WindowedListSimulatorTest {

    private fun createSimulator(
        itemCount: Int = 100,
        itemHeight: Double = 80.0,
        gap: Double = 8.0,
        viewportSize: Double = 600.0
    ): WindowedListSimulator {
        val sim = WindowedListSimulator(defaultItemHeight = itemHeight, gap = gap)
        sim.viewportSize = viewportSize
        sim.setData(List(itemCount) { itemHeight })
        return sim
    }

    private fun createVariableHeightSimulator(
        itemCount: Int = 100,
        gap: Double = 8.0,
        viewportSize: Double = 600.0,
        heightFn: (Int) -> Double = { 40.0 + (it % 8) * 60.0 }
    ): WindowedListSimulator {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = gap)
        sim.viewportSize = viewportSize
        sim.setData(List(itemCount) { heightFn(it) })
        return sim
    }

    private fun WindowedListSimulator.assertInvariants(context: String = "") {
        val prefix = if (context.isNotEmpty()) "$context: " else ""
        val violations = checkInvariants()
        assertTrue(violations.isEmpty(), "${prefix}Invariant violations: ${violations.joinToString("; ")}\nState: $this")
    }

    private fun WindowedListSimulator.assertSettled(context: String = "") {
        val prefix = if (context.isNotEmpty()) "$context: " else ""
        assertTrue(abs(currentTransform) < 1.0, "${prefix}Transform not settled: $currentTransform\nState: $this")
    }

    // === Basic initialization tests === by Claude

    @Test
    fun testEmptyList() {
        val sim = WindowedListSimulator()
        sim.viewportSize = 600.0
        sim.setData(emptyList())

        assertEquals(0, sim.renderedItems.size, "No items should be rendered for empty list")
        assertEquals(0.0, sim.topSpacerSize)
        assertEquals(0.0, sim.bottomSpacerSize)
        assertEquals(0.0, sim.currentTransform)
    }

    @Test
    fun testSingleItem() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0
        sim.setData(listOf(80.0))

        assertEquals(1, sim.renderedItems.size)
        assertEquals(0, sim.renderedItems[0].index)
        sim.assertInvariants("single item")
    }

    @Test
    fun testTwoItems() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0
        sim.setData(listOf(80.0, 80.0))

        assertEquals(2, sim.renderedItems.size)
        assertEquals(0, sim.renderedItems[0].index)
        assertEquals(1, sim.renderedItems[1].index)
        sim.assertInvariants("two items")
    }

    @Test
    fun testInitialFill() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Should render enough items to fill viewport + buffer
        assertTrue(sim.renderedItems.isNotEmpty(), "Should render some items")
        assertEquals(0, sim.renderedItems.first().index, "Should start at index 0")
        sim.assertInvariants("initial fill")
    }

    @Test
    fun testItemsFitInViewport() {
        // All items fit within the viewport — all should be rendered
        val sim = createSimulator(itemCount = 5, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        assertEquals(5, sim.renderedItems.size, "All 5 items should be rendered")
        assertEquals(0, sim.renderedItems.first().index)
        assertEquals(4, sim.renderedItems.last().index)
        assertEquals(0.0, sim.bottomSpacerSize, "No bottom spacer needed when all items rendered")
        sim.assertInvariants("all fit")
    }

    // === Scrolling tests === by Claude

    @Test
    fun testScrollDownIncrementally() {
        val sim = createSimulator(itemCount = 200, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Scroll down in small increments
        val step = 100.0
        for (pos in (step.toInt()..(80 * 200 + 8 * 199)).step(step.toInt())) {
            sim.scrollAndSettle(pos.toDouble())
            sim.assertInvariants("scroll to $pos")
            sim.assertSettled("scroll to $pos")

            // The viewport should be covered
            if (sim.renderedItems.isNotEmpty()) {
                val cStart = sim.contentVisualStart()
                val cEnd = sim.contentVisualEnd()
                // Rendered content should extend beyond viewport in both directions (buffer)
                // or be at a data boundary
                val firstIdx = sim.renderedItems.first().index
                val lastIdx = sim.renderedItems.last().index
                if (firstIdx > 0) {
                    assertTrue(cStart <= pos, "Content start $cStart should be <= viewport start $pos")
                }
                if (lastIdx < sim.totalItemCount - 1) {
                    assertTrue(cEnd >= pos + 600.0, "Content end $cEnd should be >= viewport end ${pos + 600.0}")
                }
            }
        }
    }

    @Test
    fun testScrollUpFromMiddle() {
        val sim = createSimulator(itemCount = 200, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Jump to middle
        sim.scrollAndSettle(5000.0)
        sim.assertInvariants("jump to middle")
        sim.assertSettled("jump to middle")

        // Scroll back up
        val step = 100.0
        var pos = 5000.0
        while (pos > 0) {
            pos -= step
            sim.scrollAndSettle(pos)
            sim.assertInvariants("scroll up to $pos")
            sim.assertSettled("scroll up to $pos")
        }
    }

    @Test
    fun testJumpToEnd() {
        val sim = createSimulator(itemCount = 1000, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Jump to the very end
        val totalHeight = sim.estimateTotalHeight()
        sim.scrollAndSettle(totalHeight - sim.viewportSize)
        sim.assertInvariants("jump to end")
        sim.assertSettled("jump to end")

        // Should have the last item rendered
        assertEquals(999, sim.renderedItems.last().index, "Last item should be rendered")
    }

    @Test
    fun testJumpToStart() {
        val sim = createSimulator(itemCount = 1000, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Jump to middle first
        sim.scrollAndSettle(5000.0)

        // Jump back to start
        sim.scrollAndSettle(0.0)
        sim.assertInvariants("jump to start")
        sim.assertSettled("jump to start")

        assertEquals(0, sim.renderedItems.first().index, "First item should be rendered")
    }

    @Test
    fun testScrollPastEnd() {
        val sim = createSimulator(itemCount = 50, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Try scrolling way past the end
        val totalHeight = sim.estimateTotalHeight()
        sim.scrollAndSettle(totalHeight + 1000.0)
        sim.assertInvariants("scroll past end")
    }

    @Test
    fun testScrollBeforeStart() {
        val sim = createSimulator(itemCount = 50, itemHeight = 80.0, gap = 8.0, viewportSize = 600.0)

        // Scroll to negative position (should be harmless)
        sim.scrollAndSettle(-100.0)
        sim.assertInvariants("scroll before start")
    }

    // === Variable height tests === by Claude

    @Test
    fun testVariableHeights() {
        val sim = createVariableHeightSimulator(itemCount = 200)

        // Scroll through the entire list
        val step = 150.0
        var pos = 0.0
        val totalHeight = sim.estimateTotalHeight()
        while (pos < totalHeight) {
            sim.scrollAndSettle(pos)
            sim.assertInvariants("variable heights at $pos")
            sim.assertSettled("variable heights at $pos")
            pos += step
        }
    }

    @Test
    fun testExtremeHeightVariation() {
        // Heights from 10 to 500 pixels
        val sim = createVariableHeightSimulator(
            itemCount = 100,
            heightFn = { 10.0 + (it % 10) * 54.0 }
        )

        sim.scrollAndSettle(0.0)
        sim.assertInvariants("extreme heights start")

        // Jump around
        sim.scrollAndSettle(2000.0)
        sim.assertInvariants("extreme heights mid")

        sim.scrollAndSettle(sim.estimateTotalHeight() - sim.viewportSize)
        sim.assertInvariants("extreme heights end")
    }

    // === Data mutation tests === by Claude

    @Test
    fun testInsertAtStartWhileScrolledToMiddle() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)
        sim.scrollAndSettle(2000.0)
        val firstRenderedBefore = sim.renderedItems.first().index

        // Insert 10 items at start (heights increase by 10)
        val newData = List(10) { 80.0 } + sim.data
        sim.setData(newData)
        sim.runLayout()
        sim.performSettle()
        sim.assertInvariants("after insert at start")

        // Rendered items should still be contiguous
        for (i in 1 until sim.renderedItems.size) {
            assertEquals(
                sim.renderedItems[i - 1].index + 1,
                sim.renderedItems[i].index,
                "Items should be contiguous after insert"
            )
        }
    }

    @Test
    fun testRemoveIncludingVisible() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)
        sim.scrollAndSettle(1000.0)

        // Remove many items, including some that are visible
        val newData = sim.data.filterIndexed { idx, _ -> idx < 5 || idx > 50 }
        sim.setData(newData)
        sim.runLayout()
        sim.performSettle()
        sim.assertInvariants("after remove visible items")
    }

    @Test
    fun testReplaceEntireDataset() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)
        sim.scrollAndSettle(2000.0)

        // Replace all data with different heights
        sim.setData(List(75) { 100.0 })
        sim.runLayout()
        sim.performSettle()
        sim.assertInvariants("after replace dataset")
    }

    @Test
    fun testEmptyToPopulatedToEmpty() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0

        // Start empty
        sim.setData(emptyList())
        assertEquals(0, sim.renderedItems.size)

        // Populate
        sim.setData(List(50) { 80.0 })
        assertTrue(sim.renderedItems.isNotEmpty(), "Should have items after populating")
        sim.assertInvariants("after populate")

        // Empty again
        sim.setData(emptyList())
        assertEquals(0, sim.renderedItems.size, "Should be empty after clearing")
        assertEquals(0.0, sim.topSpacerSize)
        assertEquals(0.0, sim.bottomSpacerSize)
    }

    @Test
    fun testGrowFromOneItem() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0

        sim.setData(listOf(80.0))
        assertEquals(1, sim.renderedItems.size)

        // Grow to many items
        sim.setData(List(100) { 80.0 })
        sim.runLayout()
        sim.assertInvariants("after grow")
        assertTrue(sim.renderedItems.size > 1, "Should render more than 1 item after growing")
    }

    @Test
    fun testShrinkToOneItem() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)
        sim.scrollAndSettle(2000.0)

        // Shrink to 1 item
        sim.setData(listOf(80.0))
        sim.runLayout()
        sim.performSettle()

        assertTrue(sim.renderedItems.size <= 1, "Should render at most 1 item")
        if (sim.renderedItems.isNotEmpty()) {
            assertEquals(0, sim.renderedItems.first().index)
        }
        sim.assertInvariants("after shrink to 1")
    }

    // === Settle behavior tests === by Claude

    @Test
    fun testSettleZerosTransform() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)

        // Prepend should create negative transform
        sim.scrollAndSettle(500.0)
        // After settle, transform should be zero
        sim.assertSettled("after settle")
    }

    @Test
    fun testSettleWithFirstItemRendered() {
        val sim = createSimulator(itemCount = 100, itemHeight = 80.0)

        // When item 0 is rendered, top spacer should be exactly 0
        sim.scrollAndSettle(0.0)
        sim.assertSettled()
        if (sim.renderedItems.any { it.index == 0 }) {
            assertEquals(0.0, sim.topSpacerSize, "Top spacer should be 0 when item 0 is rendered")
        }
    }

    // === Contiguity tests === by Claude

    @Test
    fun testContiguityDuringRapidScrolling() {
        val sim = createSimulator(itemCount = 500, itemHeight = 80.0, viewportSize = 600.0)

        // Simulate rapid random-ish scrolling
        val positions = listOf(0.0, 5000.0, 2000.0, 15000.0, 100.0, 30000.0, 10000.0, 0.0)
        for (pos in positions) {
            sim.scrollAndSettle(pos)
            sim.assertInvariants("rapid scroll to $pos")
            sim.assertSettled("rapid scroll to $pos")
        }
    }

    // === Large dataset tests === by Claude

    @Test
    fun testLargeDataset() {
        val sim = WindowedListSimulator(defaultItemHeight = 40.0, gap = 4.0)
        sim.viewportSize = 800.0
        sim.setData(List(10000) { 40.0 })

        sim.assertInvariants("large dataset initial")

        // Scroll to various positions
        sim.scrollAndSettle(100000.0)
        sim.assertInvariants("large dataset middle")
        sim.assertSettled("large dataset middle")

        val totalHeight = sim.estimateTotalHeight()
        sim.scrollAndSettle(totalHeight - sim.viewportSize)
        sim.assertInvariants("large dataset end")
        assertEquals(9999, sim.renderedItems.last().index, "Should reach last item")
    }

    // === Trim tests === by Claude

    @Test
    fun testTrimRemovesDistantItems() {
        val sim = createSimulator(itemCount = 500, itemHeight = 80.0, viewportSize = 600.0)

        // Fill a large window
        sim.scrollAndSettle(0.0)
        val initialCount = sim.renderedItems.size

        // Scroll far away — items from the old position should be trimmed
        sim.scrollAndSettle(20000.0)
        sim.assertInvariants("after far scroll")

        // The rendered range should no longer include items near index 0
        assertTrue(sim.renderedItems.first().index > 0, "Should have trimmed items near the top")
    }

    @Test
    fun testTrimKeepsAtLeastOneItem() {
        val sim = createSimulator(itemCount = 3, itemHeight = 80.0, viewportSize = 600.0)

        // Even with aggressive scrolling, should keep at least one item
        sim.scrollAndSettle(10000.0)
        assertTrue(sim.renderedItems.isNotEmpty(), "Should always keep at least one item")
    }

    // === Spacer consistency tests === by Claude

    @Test
    fun testSpacerConsistencyAfterSettle() {
        val sim = createSimulator(itemCount = 200, itemHeight = 80.0)

        // After settle at various positions, spacers should be consistent
        for (pos in listOf(0.0, 1000.0, 5000.0, 10000.0)) {
            sim.scrollAndSettle(pos)
            sim.assertInvariants("spacer consistency at $pos")
            sim.assertSettled("spacer consistency at $pos")

            // Top spacer + rendered content + bottom spacer should approximately equal total height
            // Tolerance accounts for gap boundary effects between settle (precise) and estimate methods
            if (sim.renderedItems.isNotEmpty()) {
                val total = sim.topSpacerSize + sim.renderedContentHeight() + sim.bottomSpacerSize
                val estimated = sim.estimateTotalHeight()
                val tolerance = 12.0  // gap (8) + small epsilon
                assertTrue(
                    abs(total - estimated) < tolerance,
                    "Total height mismatch at pos=$pos: spacers+content=$total estimated=$estimated (diff=${abs(total - estimated)})"
                )
            }
        }
    }

    // === Sequential operations tests === by Claude

    @Test
    fun testInsertRemoveSequence() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0
        sim.setData(List(50) { 80.0 })
        sim.scrollAndSettle(0.0)

        // Insert at end repeatedly
        for (i in 50 until 60) {
            val newData = sim.data + 80.0
            sim.setData(newData.toList())
            sim.runLayout()
        }
        sim.performSettle()
        sim.assertInvariants("after repeated inserts")

        // Remove from start repeatedly
        for (i in 0 until 10) {
            sim.setData(sim.data.drop(1))
            sim.runLayout()
        }
        sim.performSettle()
        sim.assertInvariants("after repeated removes")
    }

    @Test
    fun testAlternatingInsertRemove() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 600.0
        sim.setData(List(50) { 80.0 })
        sim.scrollAndSettle(1000.0)

        // Alternate insert and remove
        for (i in 0 until 20) {
            if (i % 2 == 0) {
                // Insert at middle
                val mList = sim.data.toMutableList()
                val idx = (mList.size / 2).coerceIn(0, mList.size)
                mList.add(idx, 80.0)
                sim.setData(mList)
            } else {
                // Remove from start
                if (sim.data.isNotEmpty()) {
                    sim.setData(sim.data.drop(1))
                }
            }
            sim.runLayout()
        }
        sim.performSettle()
        sim.assertInvariants("after alternating insert/remove")
    }

    // === Edge cases === by Claude

    @Test
    fun testZeroViewportSize() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 0.0
        sim.setData(List(10) { 80.0 })

        // Initial render should still happen even with zero viewport (SSR case)
        assertEquals(10, sim.renderedItems.size, "All 10 items should be rendered via initialRenderCount")
        sim.assertInvariants("zero viewport")
    }

    // === SSR / initial render tests === by Claude

    @Test
    fun testInitialRenderBeforeViewport() {
        // Simulate SSR: viewport is zero, data arrives
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 15)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        // Should render exactly initialRenderCount items even without viewport
        assertEquals(15, sim.renderedItems.size, "Should render initialRenderCount items")
        assertEquals(0, sim.renderedItems.first().index)
        assertEquals(14, sim.renderedItems.last().index)
        sim.assertInvariants("SSR initial")
    }

    @Test
    fun testInitialRenderCountExceedsData() {
        // initialRenderCount larger than data — should render all items
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 20)
        sim.viewportSize = 0.0
        sim.setData(List(5) { 80.0 })

        assertEquals(5, sim.renderedItems.size, "Should render all items when data < initialRenderCount")
        sim.assertInvariants("SSR small data")
    }

    @Test
    fun testInitialRenderThenViewportArrives() {
        // SSR renders items, then viewport becomes available and layout runs
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 5)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })
        assertEquals(5, sim.renderedItems.size, "Initial render with no viewport")

        // Viewport becomes available — layout should expand the buffer
        sim.viewportSize = 600.0
        sim.scrollAndSettle(0.0)
        assertTrue(sim.renderedItems.size > 5, "Layout should render more items once viewport is available")
        sim.assertInvariants("after viewport arrives")
    }

    @Test
    fun testInitialRenderCountZero() {
        // initialRenderCount = 0 means no pre-rendering
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 0)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        assertEquals(0, sim.renderedItems.size, "No items rendered when initialRenderCount=0 and no viewport")
    }

    // === initialRenderIndex tests === by Claude

    @Test
    fun testInitialRenderFromMiddle() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 50)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        assertEquals(10, sim.renderedItems.size)
        assertEquals(50, sim.renderedItems.first().index, "Should start at initialRenderIndex")
        assertEquals(59, sim.renderedItems.last().index)
        // Top spacer should reflect the estimated height of items 0..49
        assertTrue(sim.topSpacerSize > 0.0, "Top spacer should account for items above initialRenderIndex")
        sim.assertInvariants("render from middle")
    }

    @Test
    fun testInitialRenderFromMiddleThenScrollUp() {
        // Start with no viewport so initial render happens at index 50 without runLayout overriding it
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 50)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        val firstRendered = sim.renderedItems.first().index
        assertTrue(firstRendered > 0, "Should not have rendered from the start")

        // Viewport arrives positioned at the rendered content
        sim.viewportSize = 600.0
        val contentPos = sim.topSpacerSize
        sim.scrollAndSettle(contentPos)
        sim.assertInvariants("after viewport at content")

        // Scroll up — items above should be lazily prepended
        sim.scrollAndSettle(contentPos - 400.0)
        sim.assertInvariants("scroll up from middle")
        assertTrue(
            sim.renderedItems.first().index < firstRendered,
            "Scrolling up should prepend items above initial range"
        )
    }

    @Test
    fun testInitialRenderFromMiddleThenScrollToTop() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 50)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })
        sim.viewportSize = 600.0

        // Scroll all the way to the top
        sim.scrollAndSettle(0.0)
        sim.assertInvariants("scroll to top from middle start")
        sim.assertSettled("scroll to top from middle start")
        assertEquals(0, sim.renderedItems.first().index, "Should reach item 0")
    }

    @Test
    fun testInitialRenderNearEnd() {
        // Start near the end — only a few items available after the start index
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 95)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        assertEquals(5, sim.renderedItems.size, "Should render remaining 5 items (95..99)")
        assertEquals(95, sim.renderedItems.first().index)
        assertEquals(99, sim.renderedItems.last().index)
        sim.assertInvariants("render near end")
    }

    @Test
    fun testInitialRenderIndexBeyondData() {
        // initialRenderIndex beyond data bounds — should clamp to last item
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 500)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 80.0 })

        assertTrue(sim.renderedItems.isNotEmpty(), "Should render at least something")
        assertEquals(99, sim.renderedItems.first().index, "Should clamp to last valid index")
        sim.assertInvariants("render index beyond data")
    }

    @Test
    fun testInitialRenderFromMiddleWithVariableHeights() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0, initialRenderCount = 10, initialRenderIndex = 30)
        sim.viewportSize = 0.0
        sim.setData(List(100) { 40.0 + (it % 8) * 60.0 })
        sim.viewportSize = 600.0

        sim.assertInvariants("variable heights from middle")

        // Scroll through the full range
        sim.scrollAndSettle(0.0)
        sim.assertInvariants("variable heights scrolled to top")
        assertEquals(0, sim.renderedItems.first().index)

        sim.scrollAndSettle(sim.estimateTotalHeight() - sim.viewportSize)
        sim.assertInvariants("variable heights scrolled to end")
        // With variable heights, estimates may not perfectly reach the last item,
        // but we should be near the end. by Claude
        assertTrue(sim.renderedItems.last().index >= 90, "Should be near the end, got ${sim.renderedItems.last().index}")
    }

    @Test
    fun testVerySmallViewport() {
        val sim = WindowedListSimulator(defaultItemHeight = 80.0, gap = 8.0)
        sim.viewportSize = 10.0  // Smaller than one item
        sim.setData(List(100) { 80.0 })

        sim.assertInvariants("tiny viewport")

        sim.scrollAndSettle(500.0)
        sim.assertInvariants("tiny viewport scrolled")
    }

    @Test
    fun testAllSameHeight() {
        val sim = createSimulator(itemCount = 200, itemHeight = 100.0, gap = 0.0, viewportSize = 1000.0)

        // With no gap and uniform height, positions are perfectly predictable
        sim.scrollAndSettle(0.0)
        sim.assertInvariants("uniform no-gap")

        // Item at index i should start at exactly i * 100
        if (sim.renderedItems.isNotEmpty() && sim.renderedItems.first().index == 0) {
            for ((renderedIdx, item) in sim.renderedItems.withIndex()) {
                val expectedStart = item.index * 100.0
                val actualStart = sim.renderedItemStart(renderedIdx)
                assertTrue(
                    abs(actualStart - expectedStart) < 1.0,
                    "Item ${item.index} expected at $expectedStart, got $actualStart"
                )
            }
        }
    }
}
