package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CoordinatorTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    // Shows [page] inside a coordinator frame set up the way `appBase` does it.
    private fun ComposeUiTest.show(page: CoordinatorFrame.() -> Unit): Pair<KiteUiRoot, CoordinatorFrame> {
        lateinit var frame: CoordinatorFrame
        val root = runBlocking(Dispatchers.Main) {
            KiteUiRoot("kiteui-compose-test", { theme }) {
                coordinatorFrame {
                    frame = this
                    context.overlayFrame = this
                    page()
                }
            }
        }
        setContent { root.Content() }
        waitForIdle()
        return root to frame
    }

    private fun ComposeUiTest.screenshot(root: KiteUiRoot, name: String) {
        val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
        ImageIO.write(runBlocking { root.screenshot() }.toAwtImage(), "png", File(out, "$name.png"))
    }

    private fun ComposeUiTest.bounds(text: String): DpRect = onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()
    private fun ComposeUiTest.rootBounds(): DpRect = onRoot().getUnclippedBoundsInRoot()

    // Lays children out in a diagonal, each at its measured size, inside the padding and separated by the gap.
    private object Diagonal : ProgrammaticLayoutDelegate {
        override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size = within

        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            var x = inProgress.paddingLeft
            var y = inProgress.paddingTop
            for (child in layout.children) {
                val size = inProgress.measure(child, Size(within.width, within.height))
                inProgress.place(child, x, y, x + size.width, y + size.height)
                x += size.width + inProgress.gap
                y += size.height + inProgress.gap
            }
        }
    }

    @Test
    fun programmaticLayoutPlacesChildrenWhereItsDelegateSays() = runComposeUiTest {
        lateinit var layout: ProgrammaticLayout
        val (root, _) = show {
            col {
                text("Above")
                sizeConstraints(width = 400.px, height = 300.px).card.programmatic {
                    layout = this
                    gap = 10.px
                    delegate = Diagonal
                    text("First")
                    text("Second")
                }
            }
        }
        val above = bounds("Above")
        val first = bounds("First")
        val second = bounds("Second")
        val padding = theme.padding.top.value.dp
        assertEquals(above.bottom + theme.gap.value.dp + padding, first.top, "first child should sit inside the padding")
        assertEquals(first.right + 10.dp, second.left)
        assertEquals(first.bottom + 10.dp, second.top)

        // A new child and a new delegate both lay out again.
        runOnUiThread { layout.text("Third") }
        waitForIdle()
        assertEquals(second.right + 10.dp, bounds("Third").left)
        screenshot(root, "programmatic")
        runOnUiThread { layout.delegate = ProgrammaticLayoutDelegate.AllFull }
        waitForIdle()
        assertEquals(bounds("First").left, bounds("Third").left)
        root.close()
    }

    @Test
    fun recyclerViewShowsItsFirstCells() = runComposeUiTest {
        val root = show {
            recyclerView {
                children({ (1..1000).toList() }, id = { it }) { item -> text { ::content { "Item ${item()}" } } }
            }
        }.first
        waitForIdle()
        screenshot(root, "recycler")
        onNodeWithText("Item 1", useUnmergedTree = true).assertExists()
        onNodeWithText("Item 1000", useUnmergedTree = true).assertDoesNotExist()

        // Scrolling far down recycles the first cells into later ones.
        onRoot().performMouseInput { moveTo(center); repeat(20) { scroll(10f) } }
        waitForIdle()
        screenshot(root, "recycler-scrolled")
        onNodeWithText("Item 1", useUnmergedTree = true).assertDoesNotExist()
        root.close()
    }

    @Test
    fun bottomSheetMovesBetweenStatesAndCloses()= runComposeUiTest {
        lateinit var control: BottomSheetControl
        var behindClicks = 0
        val (root, frame) = show {
            button {
                text("Page behind")
                onClick { behindClicks++ }
            }
        }
        runOnUiThread {
            frame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED, partialRatio = 0.4f) {
                control = it
                card.col {
                    centered.coordinatorDragHandle()
                    text("Sheet content")
                }
            }
        }
        waitForIdle()
        val height = rootBounds().bottom
        val partialTop = bounds("Sheet content").top
        assertTrue(partialTop > height * 0.6f - 1.dp && partialTop < height * 0.75f, "partial sheet should cover the bottom 40%, content at $partialTop of $height")
        screenshot(root, "sheet-partial")

        // The page above a partial sheet still takes clicks.
        onNodeWithText("Page behind").performClick()
        waitForIdle()
        assertEquals(1, behindClicks)

        val handle = onNode(hasClickAction() and !hasText("Page behind"))
        // The handle toggles to expanded and back, as on the web.
        handle.performClick()
        waitForIdle()
        assertEquals(BottomSheetState.EXPANDED, control.state.state.getOrNull())
        assertTrue(bounds("Sheet content").top < height * 0.2f)
        screenshot(root, "sheet-expanded")
        handle.performClick()
        waitForIdle()
        assertEquals(BottomSheetState.PARTIALLY_EXPANDED, control.state.state.getOrNull())

        // Setting the state moves it too.
        runOnUiThread { runBlocking { control.state.set(BottomSheetState.EXPANDED) } }
        waitForIdle()
        val expandedTop = bounds("Sheet content").top
        assertTrue(expandedTop < height * 0.2f)

        // The sheet follows a drag on the handle, then settles at the nearest state.
        handle.performMouseInput {
            moveTo(center)
            press()
            repeat(10) { moveBy(Offset(0f, 20f * density)) }
        }
        waitForIdle()
        val moved = bounds("Sheet content").top - expandedTop
        assertTrue(moved > 150.dp && moved < 200.dp, "the sheet should follow the pointer down 200dp minus the touch slop, moved $moved")
        handle.performMouseInput {
            moveBy(Offset(0f, height.value * 0.4f * density))
            release()
        }
        waitForIdle()
        assertEquals(BottomSheetState.PARTIALLY_EXPANDED, control.state.state.getOrNull())
        assertEquals(partialTop.value, bounds("Sheet content").top.value, 2f)

        runOnUiThread { control.close() }
        waitForIdle()
        onNodeWithText("Sheet content", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Page behind", useUnmergedTree = true).assertExists()
        root.close()
    }

    @Test
    fun draggingABottomSheetOffTheBottomClosesIt() = runComposeUiTest {
        val (root, frame) = show { text("Page behind") }
        runOnUiThread {
            frame.bottomSheet { card.col { centered.coordinatorDragHandle(); text("Sheet content") } }
        }
        waitForIdle()
        val height = rootBounds().bottom
        onNode(hasClickAction()).performMouseInput {
            moveTo(center)
            press()
            repeat(10) { moveBy(Offset(0f, height.value * 0.1f * density)) }
            release()
        }
        waitForIdle()
        onNodeWithText("Sheet content", useUnmergedTree = true).assertDoesNotExist()
        root.close()
    }


    @Test
    fun slidingPanels() = runComposeUiTest {
        lateinit var left: SlidingPanelControl
        lateinit var right: SlidingPanelControl
        val (root, frame) = show { text("Page behind") }
        runOnUiThread {
            frame.leftSlidingPanel(ratio = 0.25f) { left = it; card.col { text("Left panel") } }
            frame.rightSlidingPanel { right = it; card.col { text("Right panel") } }
        }
        waitForIdle()
        val width = rootBounds().right
        val leftPanel = bounds("Left panel")
        val rightPanel = bounds("Right panel")
        assertTrue(leftPanel.right < width * 0.25f, "left panel should take a quarter of the width, ends at ${leftPanel.right} of $width")
        assertTrue(rightPanel.left > width * 0.5f, "an unsized right panel should hug the right edge, starts at ${rightPanel.left}")
        screenshot(root, "panels")

        runOnUiThread { left.close() }
        waitForIdle()
        onNodeWithText("Left panel", useUnmergedTree = true).assertDoesNotExist()
        runOnUiThread { right.close() }
        waitForIdle()
        onNodeWithText("Right panel", useUnmergedTree = true).assertDoesNotExist()
        root.close()
    }

    @Test
    fun overlayDialogRendersOnTopAndCloses() = runComposeUiTest {
        var behindClicks = 0
        val (root, frame) = show {
            button {
                text("Behind")
                onClick { behindClicks++ }
            }
        }
        runOnUiThread {
            frame.context.overlay(modal = true) { close ->
                centered.card.col {
                    text("Are you sure?")
                    button {
                        text("Close dialog")
                        onClick { close() }
                    }
                }
            }
        }
        waitForIdle()
        screenshot(root, "dialog")

        // The dialog card covers what's behind it, even where it has no button of its own.
        val title = bounds("Are you sure?")
        onRoot().performMouseInput { click(Offset((title.left.value + 1f) * density, (title.top.value + 1f) * density)) }
        waitForIdle()
        assertEquals(0, behindClicks, "a click on the dialog reached the page behind it")

        onNodeWithText("Close dialog").performClick()
        waitForIdle()
        onNodeWithText("Are you sure?", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Behind").performClick()
        waitForIdle()
        assertEquals(1, behindClicks)
        root.close()
    }

    @Test
    fun toastAndLegacyBottomSheet() = runComposeUiTest {
        val (root, frame) = show { text("Page behind") }
        runOnUiThread { frame.context.toast("Saved") }
        waitForIdle()
        onNodeWithText("Saved", useUnmergedTree = true).assertExists()
        screenshot(root, "toast")

        @Suppress("DEPRECATION")
        runOnUiThread { frame.context.openBottomSheet { text("Legacy sheet") } }
        waitForIdle()
        assertTrue(bounds("Legacy sheet").top > rootBounds().bottom * 0.4f)
        screenshot(root, "legacy-sheet")
        onNodeWithContentDescription("close", useUnmergedTree = true).performClick()
        waitForIdle()
        onNodeWithText("Legacy sheet", useUnmergedTree = true).assertDoesNotExist()
        root.close()
    }
}
