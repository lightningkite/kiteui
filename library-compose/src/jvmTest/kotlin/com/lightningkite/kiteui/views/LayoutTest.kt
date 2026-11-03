package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LayoutTest {
    // CSS break-all breaks inside words that would fit on the next line, so it needs fewer lines.
    @Test
    fun breakAllBreaksInsideWords() = runKiteUiTest {
        val content = "abcdefghijklmn ".repeat(8).trim()
        lateinit var normal: TextView
        lateinit var breakAll: TextView
        showKiteUi {
            col {
                sizeConstraints(width = 150.px).col {
                    normal = text(content)
                    breakAll = text(content).apply { wordBreak = WordBreak.BreakAll }
                }
            }
        }
        val normalHeight = runOnUiThread { normal.screenRectangle()!!.let { it.bottom - it.top } }
        val breakAllHeight = runOnUiThread { breakAll.screenRectangle()!!.let { it.bottom - it.top } }
        assertTrue(breakAllHeight < normalHeight, "break-all should fill its lines, taking $breakAllHeight against $normalHeight")
    }

    @Test
    fun textWrapsInsideAWrappingRow() = runKiteUiTest {
        val long = "word ".repeat(80)
        showKiteUi {
            col {
                text("One line")
                row {
                    text("Label")
                    expanding.text(long)
                }
            }
        }
        val oneLine = onNodeWithText("One line").getUnclippedBoundsInRoot().height
        val wrapped = onNodeWithText(long.trim(), substring = true).getUnclippedBoundsInRoot().height
        assertTrue(wrapped > oneLine * 2, "Expected the long text to wrap onto several lines, got $wrapped vs one line $oneLine")
    }

    // Like the web's scrollIntoView: an alignment says where the element ends up, null scrolls as little as
    // possible, and animate decides whether it jumps.
    @Test
    fun scrollIntoViewHonoursAlignmentAndAnimate() = runKiteUiTest {
        mainClock.autoAdvance = false
        val items = ArrayList<TextView>()
        showKiteUi {
            col {
                sizeConstraints(height = 300.px).scrolling.col {
                    gap = 0.px
                    for (i in 0 until 50) items += sizeConstraints(height = 40.px).text("Item $i")
                }
            }
        }
        mainClock.advanceTimeBy(400)
        val viewport = onNodeWithText("Item 0").getUnclippedBoundsInRoot().top
        fun bounds(i: Int) = onNodeWithText("Item $i").getUnclippedBoundsInRoot()

        runOnUiThread { items[20].scrollIntoView(null, Align.Center, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals((viewport + 150.dp).value, ((bounds(20).top + bounds(20).bottom) / 2).value, 1f, "centered in the 300dp viewport")

        // Already in view, so null doesn't move it.
        val before = bounds(21).top
        runOnUiThread { items[21].scrollIntoView(null, null, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals(before, bounds(21).top)

        // Further down, null brings it just into view, at the bottom.
        runOnUiThread { items[30].scrollIntoView(null, null, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals((viewport + 300.dp).value, bounds(30).bottom.value, 1f)

        runOnUiThread { items[10].scrollIntoView(null, Align.Start, animate = true) }
        mainClock.advanceTimeBy(48)
        val moving = bounds(10).top
        assertTrue(moving < viewport, "an animated scroll should still be on its way, at $moving")
        mainClock.advanceTimeBy(1000)
        assertEquals(viewport.value, bounds(10).top.value, 1f)
    }

    // As with flex (and Android's fillViewport), scrolled content is at least as wide as the viewport,
    // so expanding spaces around narrow content center it.
    @Test
    fun weightsFillAScrollViewport() = runKiteUiTest {
        showKiteUi {
            col {
                scrollingHorizontally.row {
                    expanding.space()
                    text("Middle")
                    expanding.space()
                }
            }
        }
        val root = onRoot().getUnclippedBoundsInRoot()
        val middle = onNodeWithText("Middle").getUnclippedBoundsInRoot()
        val center = (middle.left + middle.right) / 2
        assertEquals((root.width / 2).value, center.value, 2f, "Expected the text centered in the viewport")
    }

    @Test
    fun weightsShareTheRemainingSpace() = runKiteUiTest {
        showKiteUi {
            row {
                sizeConstraints(width = 100.px).text("Fixed")
                weight(1f).text("One")
                weight(3f).text("Three")
            }
        }
        val one = onNodeWithText("One").getUnclippedBoundsInRoot().width
        val three = onNodeWithText("Three").getUnclippedBoundsInRoot().width
        assertTrue(one > 0.dp, "weighted child got no space")
        assertEquals(3.0, (three / one).toDouble(), 0.05)
    }

    @Test
    fun aspectRatioFollowsAHeightStretchedByARow() = runKiteUiTest {
        var box: Frame? = null
        showKiteUi {
            col {
                sizeConstraints(height = 100.px).row {
                    sizeConstraints(aspectRatio = 2 to 1).frame { box = this }
                    expanding.space()
                }
            }
        }
        val bounds = box!!.screenRectangle()!!
        assertEquals(100.0, bounds.height, 1.0)
        assertEquals(200.0, bounds.width, 1.0)
    }

    // As on the web, every view a swap writes is shown, and goes at the next swap.
    @Test
    fun swapViewShowsAndRemovesEveryViewWritten() = runKiteUiTest {
        lateinit var swapView: SwapView
        lateinit var first: TextView
        lateinit var second: TextView
        showKiteUi {
            col {
                swapView = swapView {
                    swap {
                        first = text("First")
                        second = text("Second")
                    }
                }
            }
        }
        onNodeWithText("First", useUnmergedTree = true).assertExists()
        onNodeWithText("Second", useUnmergedTree = true).assertExists()
        runOnUiThread { swapView.swap { text("Replacement") } }
        waitForIdle()
        onNodeWithText("First", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Second", useUnmergedTree = true).assertDoesNotExist()
        assertTrue(first.isShutdown && second.isShutdown, "the replaced views should be shut down")
    }

    @Test
    fun swapViewKeepsShownWhenInCharge() = runKiteUiTest {
        val visible = Signal(false)
        showKiteUi {
            col {
                shownWhen { visible() }.swapView { swap { text("Swapped in") } }
            }
        }
        onNodeWithText("Swapped in", useUnmergedTree = true).assertDoesNotExist()
        visible.value = true
        // shownWhen reacts on the Swing thread, which waitForIdle doesn't track.
        waitUntil { onAllNodesWithText("Swapped in", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
