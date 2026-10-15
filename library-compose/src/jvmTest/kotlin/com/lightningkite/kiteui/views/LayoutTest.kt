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
