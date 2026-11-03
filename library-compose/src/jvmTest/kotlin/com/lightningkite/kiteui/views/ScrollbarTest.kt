package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollbarTest {
    private fun ComposeUiTest.showLongList(showScrollBars: Boolean) = showKiteUi {
        sizeConstraints(width = 300.px, height = 200.px).scrolling(vertical = true, horizontal = false) {
            this.showScrollBars = showScrollBars
        }.col {
            repeat(100) { text("Line $it") }
        }
    }

    // Drags from the top of the list's right edge, where the vertical thumb starts.
    private fun ComposeUiTest.dragThumbDown() {
        val list = onNodeWithText("Line 0").onParent().getBoundsInRoot()
        val x = (list.right.value - 5f)
        val y = (list.top.value + 10f)
        onRoot().performMouseInput {
            moveTo(Offset(x, y))
            press()
            moveTo(Offset(x, y + 100f))
            release()
        }
        waitForIdle()
    }

    private fun ComposeUiTest.firstLineTop() = onNodeWithText("Line 0").getUnclippedBoundsInRoot().top.value

    @Test
    fun draggingTheThumbScrolls() = runKiteUiTest {
        val root = showLongList(showScrollBars = true)
        val before = firstLineTop()
        dragThumbDown()
        saveScreenshot(root, "scrollbar-dragged")
        // The content is about 10 viewports tall, so the thumb moving half a viewport scrolls several viewports.
        assertTrue(before - firstLineTop() > 400f, "Expected a long scroll, moved ${before - firstLineTop()}")
    }

    @Test
    fun hiddenScrollbarsDontDrag() = runKiteUiTest {
        showLongList(showScrollBars = false)
        val before = firstLineTop()
        dragThumbDown()
        assertEquals(before, firstLineTop())
    }
}
