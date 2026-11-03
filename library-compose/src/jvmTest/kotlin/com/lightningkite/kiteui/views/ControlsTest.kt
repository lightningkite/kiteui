package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.LinkAnnotation
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ControlsTest {
    private fun ComposeUiTest.show(app: ViewWriter.() -> Unit): KiteUiRoot = showKiteUi {
        frame {
            context.overlayFrame = this
            app()
        }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun togglesReachTheirSignals() = runKiteUiTest {
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
        saveScreenshot(root, "toggles-off")

        onNode(hasRole(Role.Checkbox)).performClick()
        onNode(hasRole(Role.Switch)).performClick()
        onNode(hasRole(Role.RadioButton) and hasText("Radio toggle").not()).performClick()
        onNodeWithText("Toggle").performClick()
        onNodeWithText("Radio toggle").performClick()
        waitForIdle()
        assertTrue(checkbox.value && switch.value && radio.value && toggle.value && radioToggle.value, "a click didn't reach its signal")
        onNode(hasRole(Role.Checkbox)).assertIsOn()
        saveScreenshot(root, "toggles-on")

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
    }

    @Test
    fun sliderClampsAndSteps() = runKiteUiTest {
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
        saveScreenshot(root, "slider")
    }

    @Test
    fun sliderTakesTheHoverTheme() = runKiteUiTest {
        lateinit var slider: Slider
        show { col { slider = slider { } } }
        val rest = slider.themeAndBack
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performMouseInput { moveTo(center) }
        waitForIdle()
        assertTrue(slider.themeAndBack != rest, "hovering should change the slider's theme")
        onRoot().performMouseInput { moveTo(Offset(width - 1f, height - 1f)) }
        waitForIdle()
        assertEquals(rest, slider.themeAndBack)
    }

    // range sets min first, above the old max.  As the web's range input, the value then sits at min.
    @Test
    fun sliderTakesRangesAboveOrOnTheOldOne() = runKiteUiTest {
        lateinit var above: Slider
        lateinit var empty: Slider
        val root = show {
            col {
                above = slider { range(18f, 100f) }
                empty = slider { range(5, 5) }
            }
        }
        assertEquals(18f, above.value.value)
        assertEquals(5f, empty.value.value)
        // An empty range draws its thumb at the start, as the web does.
        val node = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[1]
        val bounds = node.getUnclippedBoundsInRoot()
        val image = saveScreenshot(root, "slider-empty")
        val y = with(density) { (bounds.top + 2.dp).roundToPx() }
        val start = image.getRGB(with(density) { (bounds.left + 8.dp).roundToPx() }, y)
        val end = image.getRGB(with(density) { (bounds.right - 8.dp).roundToPx() }, y)
        assertTrue(start != end, "expected the thumb at the start")
    }

    // An HTML range input treats a step that isn't positive as 1, and every platform follows it.
    @Test
    fun sliderStepsOfZeroAreOnes() = runKiteUiTest {
        lateinit var slider: Slider
        show { slider = slider { range(0f, 10f, 0f) } }
        assertEquals(1f, slider.step)
        slider.value.value = 3.4f
        assertEquals(3f, slider.value.value)
    }

    @Test
    fun sliderFollowsAReactiveMax() = runKiteUiTest {
        val max = Signal(10f)
        lateinit var slider: Slider
        val root = show {
            col {
                slider = slider { reactive { this@slider.max = max() } }
            }
        }
        slider.value.value = 5f
        val node = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        val bounds = node.getUnclippedBoundsInRoot()
        // Just above the track, where only the thumb draws.
        fun thumbIsAt(fraction: Float): Boolean {
            val image = saveScreenshot(root, "slider-max")
            val radius = 8.dp
            val x = bounds.left + radius + (bounds.width - radius * 2) * fraction
            val y = bounds.top + radius - 6.dp
            val pixel = image.getRGB(with(density) { x.roundToPx() }, with(density) { y.roundToPx() })
            val background = image.getRGB(with(density) { bounds.left.roundToPx() }, with(density) { y.roundToPx() })
            return pixel != background
        }
        assertTrue(thumbIsAt(0.5f) && !thumbIsAt(0.25f))
        max.value = 20f
        waitUntil(timeoutMillis = 2_000) { node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].range.endInclusive == 20f }
        assertTrue(thumbIsAt(0.25f) && !thumbIsAt(0.5f), "the thumb didn't move when max changed")
    }

    @Test
    fun progressIndicators() = runKiteUiTest {
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
        saveScreenshot(root, "progress")
    }

    @Test
    fun linksNavigateAndRejectUnsafeUrls() = runKiteUiTest {
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
    }

    @Test
    fun htmlLinksOpenOnlySafeUrls() {
        val opened = ArrayList<String>()
        val text = parseBasicHtml(
            """<a href="https://example.com">safe</a> <a href="file:///etc/passwd">file</a> <a href="javascript:alert(1)">script</a>""",
        ) { opened += it }
        assertEquals("safe file script", text.text.trim())
        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("safe", text.text.substring(link.start, link.end))
        (link.item as LinkAnnotation.Clickable).linkInteractionListener!!.onClick(link.item)
        assertEquals(listOf("https://example.com"), opened)
    }

    @Test
    fun menuButtonOpensOnClickAndClosesOutside() = runKiteUiTest {
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
        saveScreenshot(root, "menu-open")

        onNodeWithText("Above").performClick()
        waitForIdle()
        onNodeWithText("First item").assertDoesNotExist()
    }

    @Test
    fun menuButtonOpensOnHoverUnlessClickRequired() = runKiteUiTest {
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
    }

    @Test
    fun textPopoverShowsWhileHovered() = runKiteUiTest {
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
        saveScreenshot(root, "hint")
        onNodeWithText("Elsewhere").performMouseInput { moveTo(center) }
        waitUntilDoesNotExist(hasText("A helpful hint"))
    }

    @Test
    fun hintPopoverOnAMenuButton() = runKiteUiTest {
        show {
            col {
                text("Elsewhere")
                centered.textPopover("Menu hint").menuButton {
                    requireClick = true
                    opensMenu { text("Menu item") }
                    text("Menu with a hint")
                }
            }
        }
        onNodeWithText("Menu with a hint").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Menu hint").assertExists()
        onNodeWithText("Menu with a hint").performClick()
        waitForIdle()
        onNodeWithText("Menu item").assertExists()
    }

    @Test
    fun openPopoverFromAButton() = runKiteUiTest {
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
    }

    @Test
    fun popoverClosesWhenItsAnchorLeaves() = runKiteUiTest {
        val anchorShown = Signal(true)
        show {
            col {
                shownWhen { anchorShown() }.button {
                    text("Open")
                    onClick { openPopover(PopoverPreferredDirection.belowCenter) { text("Popped") } }
                }
            }
        }
        onNodeWithText("Open").performClick()
        waitForIdle()
        onNodeWithText("Popped").assertExists()
        anchorShown.value = false
        // shownWhen reacts on the Swing thread, which waitForIdle doesn't track.
        waitUntil { onAllNodesWithText("Popped").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun dismissBackgroundIgnoresClicksOnItsContent() = runKiteUiTest {
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
        saveScreenshot(root, "dismiss-background")
    }

    // Like the web's visibility:hidden, which children inherit.
    @Test
    fun anInvisibleContainersChildrenTakeNoInput() = runKiteUiTest {
        var clicks = 0
        lateinit var panel: Element
        show {
            col {
                text("Elsewhere")
                panel = card.col {
                    button {
                        text("Hidden button")
                        onClick { clicks++ }
                    }
                }
            }
        }
        val buttonCenter = onNodeWithText("Hidden button").fetchSemanticsNode().boundsInRoot.center
        runBlocking(Dispatchers.Main) { panel.visible = false }
        waitForIdle()
        onNodeWithText("Hidden button").assert(hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)))
        onRoot().performMouseInput { click(buttonCenter) }
        waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun clicksPassThroughAnOverlayIgnoringInteraction() = runKiteUiTest {
        var clicks = 0
        show {
            frame {
                button {
                    text("Behind")
                    onClick { clicks++ }
                }
                frame { ignoreInteraction = true }
            }
        }
        onNodeWithText("Behind").performClick()
        waitForIdle()
        assertEquals(1, clicks)
    }
}
