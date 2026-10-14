package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.pageNavigator
import androidx.compose.ui.state.ToggleableState
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ControlsTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    private fun ComposeUiTest.show(app: ViewWriter.() -> Unit): KiteUiRoot {
        val root = KiteUiRoot("kiteui-compose-test", { theme }) {
            frame {
                context.overlayFrame = this
                app()
            }
        }
        setContent { root.Content() }
        waitForIdle()
        return root
    }

    private fun ComposeUiTest.screenshot(root: KiteUiRoot, name: String) {
        waitForIdle()
        val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
        ImageIO.write(runBlocking { root.screenshot() }.toAwtImage(), "png", File(out, "$name.png"))
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun togglesReachTheirSignals() = runComposeUiTest {
        val checkbox = Signal(false)
        val switch = Signal(false)
        val radio = Signal(false)
        val toggle = Signal(false)
        val radioToggle = Signal(false)
        val root = show {
            col {
                row { centered.checkbox { checked bind checkbox }; centered.text("Checkbox") }
                row { centered.switch { checked bind switch }; centered.text("Switch") }
                row { centered.radioButton { checked bind radio }; centered.text("Radio") }
                toggleButton { checked bind toggle; text("Toggle") }
                radioToggleButton { checked bind radioToggle; text("Radio toggle") }
            }
        }
        screenshot(root, "toggles-off")

        onNode(hasRole(Role.Checkbox)).performClick()
        onNode(hasRole(Role.Switch)).performClick()
        onNode(hasRole(Role.RadioButton) and hasText("Radio toggle").not()).performClick()
        onNodeWithText("Toggle").performClick()
        onNodeWithText("Radio toggle").performClick()
        waitForIdle()
        assertTrue(checkbox.value && switch.value && radio.value && toggle.value && radioToggle.value, "a click didn't reach its signal")
        onNode(hasRole(Role.Checkbox)).assertIsOn()
        screenshot(root, "toggles-on")

        // Toggles flip back; radios stay selected.
        onNode(hasRole(Role.Checkbox)).performClick()
        onNodeWithText("Toggle").performClick()
        onNodeWithText("Radio toggle").performClick()
        waitForIdle()
        assertTrue(!checkbox.value && !toggle.value && radioToggle.value)

        // And a signal change reaches the view.
        // (bind passes values on through a coroutine, which waitForIdle doesn't know about.)
        switch.value = false
        waitUntil { onNode(hasRole(Role.Switch)).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.Off }
        root.close()
    }

    @Test
    fun sliderClampsAndSteps() = runComposeUiTest {
        lateinit var slider: Slider
        val root = show {
            col {
                slider = slider { range(0, 10, 2) }
            }
        }
        assertEquals(0f, slider.value.value, "the default, 0.5, snaps to the nearest step")
        slider.value.value = 5.2f
        assertEquals(6f, slider.value.value)
        slider.value.value = 50f
        assertEquals(10f, slider.value.value)

        val node = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        node.performSemanticsAction(SemanticsActions.SetProgress) { it(3.1f) }
        assertEquals(4f, slider.value.value)
        node.performTouchInput { click(Offset(width * 0.25f, centerY)) }
        waitForIdle()
        assertTrue(slider.value.value in 2f..4f, "clicking a quarter of the way along gave ${slider.value.value}")
        screenshot(root, "slider")
        root.close()
    }

    @Test
    fun progressIndicators() = runComposeUiTest {
        val root = show {
            col {
                progressBar { ratio = 0.4f }
                row {
                    sizeConstraints(width = 3.rem).circularProgress { ratio = 0.6f }
                    activityIndicator()
                }
            }
        }
        val ring = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[1].getUnclippedBoundsInRoot()
        assertEquals(48.dp, ring.width)
        assertEquals(48.dp, ring.height)
        val bar = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[0].getUnclippedBoundsInRoot()
        assertEquals(4.dp, bar.height)
        screenshot(root, "progress")
        root.close()
    }

    @Test
    fun linksNavigateAndRejectUnsafeUrls() = runComposeUiTest {
        val navigator = PageNavigator { Routes(listOf(), mapOf()) }
        var clicked = 0
        lateinit var external: ExternalLink
        val root = show {
            context.pageNavigator = navigator
            col {
                link {
                    to = { Page.Empty }
                    onClick { clicked++ }
                    text("Go")
                }
                external = externalLink {
                    to = "javascript:alert(1)"
                    onClick { clicked++ }
                    text("Unsafe")
                }
            }
        }
        onNodeWithText("Go").performClick()
        waitForIdle()
        assertEquals(listOf<Page>(Page.Empty), navigator.stack.value)
        assertNull(external.to)
        onNodeWithText("Unsafe").performClick()
        waitForIdle()
        assertEquals(2, clicked)
        root.close()
    }

    @Test
    fun menuButtonOpensOnClickAndClosesOutside() = runComposeUiTest {
        val root = show {
            col {
                text("Above")
                centered.menuButton {
                    requireClick = true
                    opensMenu { col { text("First item"); text("Second item") } }
                    text("Menu")
                }
            }
        }
        onNodeWithText("First item").assertDoesNotExist()
        onNodeWithText("Menu").performClick()
        waitForIdle()
        val button = onNodeWithText("Menu").getUnclippedBoundsInRoot()
        val item = onNodeWithText("First item").assertExists().getUnclippedBoundsInRoot()
        assertTrue(item.top >= button.bottom, "the menu should open below its button: $item vs $button")
        screenshot(root, "menu-open")

        onNodeWithText("Above").performClick()
        waitForIdle()
        onNodeWithText("First item").assertDoesNotExist()
        root.close()
    }

    @Test
    fun menuButtonOpensOnHoverUnlessClickRequired() = runComposeUiTest {
        val root = show {
            col {
                text("Elsewhere")
                centered.menuButton {
                    opensMenu { text("Hover item") }
                    text("Hover menu")
                }
            }
        }
        onNodeWithText("Hover menu").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Hover item").assertExists()
        // Moving onto the menu keeps it open...
        onNodeWithText("Hover item").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Hover item").assertExists()
        // ...and leaving both closes it.
        onNodeWithText("Elsewhere").performMouseInput { moveTo(center) }
        waitUntilDoesNotExist(hasText("Hover item"))
        root.close()
    }

    @Test
    fun textPopoverShowsWhileHovered() = runComposeUiTest {
        val root = show {
            col {
                text("Elsewhere")
                centered.textPopover("A helpful hint").text("Hover for a hint")
            }
        }
        onNodeWithText("A helpful hint").assertDoesNotExist()
        onNodeWithText("Hover for a hint").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("A helpful hint").assertExists()
        screenshot(root, "hint")
        onNodeWithText("Elsewhere").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("A helpful hint").assertDoesNotExist()
        root.close()
    }

    @Test
    fun openPopoverFromAButton() = runComposeUiTest {
        val root = show {
            col {
                button {
                    text("Open")
                    onClick { openPopover(PopoverPreferredDirection.belowCenter) { text("Popped") } }
                }
            }
        }
        onNodeWithText("Open").performClick()
        waitForIdle()
        onNodeWithText("Popped").assertExists()
        // A click on the popover itself doesn't close it.
        onNodeWithText("Popped").performClick()
        waitForIdle()
        onNodeWithText("Popped").assertExists()
        root.close()
    }

    @Test
    fun dismissBackgroundIgnoresClicksOnItsContent() = runComposeUiTest {
        var dismissed = 0
        val root = show {
            dismissBackground {
                onClick { dismissed++ }
                centered.card.text("Dialog content")
            }
        }
        onNodeWithText("Dialog content").performClick()
        waitForIdle()
        assertEquals(0, dismissed)
        onRoot().performTouchInput { click(Offset(5f, 5f)) }
        waitForIdle()
        assertEquals(1, dismissed)
        screenshot(root, "dismiss-background")
        root.close()
    }
}
