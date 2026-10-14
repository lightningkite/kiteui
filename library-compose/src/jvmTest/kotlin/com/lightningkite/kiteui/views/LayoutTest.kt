package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LayoutTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    private fun ComposeUiTest.show(app: ViewWriter.() -> Unit): KiteUiRoot {
        val root = runBlocking(Dispatchers.Main) { KiteUiRoot("kiteui-compose-test", { theme }, app) }
        setContent { root.Content() }
        waitForIdle()
        return root
    }

    @Test
    fun textWrapsInsideAWrappingRow() = runComposeUiTest {
        val long = "word ".repeat(80)
        val root = show {
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
        root.close()
    }

    @Test
    fun weightsShareTheRemainingSpace() = runComposeUiTest {
        val root = show {
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
        root.close()
    }

    @Test
    fun swapViewKeepsShownWhenInCharge() = runComposeUiTest {
        val visible = Signal(false)
        val root = show {
            col {
                shownWhen { visible() }.swapView { swap { text("Swapped in") } }
            }
        }
        onNodeWithText("Swapped in", useUnmergedTree = true).assertDoesNotExist()
        visible.value = true
        // shownWhen reacts on the Swing thread, which waitForIdle doesn't track.
        waitUntil { onAllNodesWithText("Swapped in", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        root.close()
    }
}
