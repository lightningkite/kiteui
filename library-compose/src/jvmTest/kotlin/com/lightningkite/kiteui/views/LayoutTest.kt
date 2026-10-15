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
