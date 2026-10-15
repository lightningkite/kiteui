package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The test theme's transitions take 250ms.  The clock is driven by hand, so the tests can look half way through.
@OptIn(ExperimentalTestApi::class)
class AnimationTest {
    private fun ComposeUiTest.bounds(text: String): DpRect = onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    // The colour just left of a text, in its container's padding.
    private fun BufferedImage.colorBeside(root: KiteUiRoot, text: DpRect): java.awt.Color =
        java.awt.Color(getRGB(((text.left.value - 3) * root.density).toInt(), ((text.top + text.bottom).value / 2 * root.density).toInt()))

    private fun swatch(red: Boolean) = ThemeDerivation {
        it.copy(id = if (red) "red" else "blue", background = if (red) Color.red else Color.blue).withBack
    }

    @Test
    fun themeChangesAnimate() = runKiteUiTest {
        mainClock.autoAdvance = false
        val red = Signal(true)
        val root = showKiteUi { col { dynamicThemed { swatch(red()) }.frame { text("Swatch") } } }
        assertEquals(255, saveScreenshot(root, "theme-start").colorBeside(root, bounds("Swatch")).red)

        runOnUiThread { red.value = false }
        mainClock.advanceTimeBy(125)
        val middle = saveScreenshot(root, "theme-middle").colorBeside(root, bounds("Swatch"))
        assertTrue(middle.red in 30..225 && middle.blue in 30..225, "Expected a colour between red and blue, got $middle")

        mainClock.advanceTimeBy(200)
        val end = saveScreenshot(root, "theme-end").colorBeside(root, bounds("Swatch"))
        assertEquals(0, end.red)
        assertEquals(255, end.blue)
    }

    @Test
    fun withoutAnimationChangesThemesInstantly() = runKiteUiTest {
        mainClock.autoAdvance = false
        val red = Signal(true)
        val root = showKiteUi { col { dynamicThemed { swatch(red()) }.frame { text("Swatch") } } }
        runOnUiThread { root.root!!.withoutAnimation { red.value = false } }
        mainClock.advanceTimeBy(16)
        assertEquals(255, saveScreenshot(root, "theme-instant").colorBeside(root, bounds("Swatch")).blue)
    }

    // As the web's CSS transitions do for :hover.
    @Test
    fun hoverAnimates() = runKiteUiTest {
        mainClock.autoAdvance = false
        val root = showKiteUi { col { button { text("Hover me") } ; text("Elsewhere") } }
        val rest = saveScreenshot(root, "hover-rest").colorBeside(root, bounds("Hover me"))

        onNodeWithText("Hover me").performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(125)
        val middle = saveScreenshot(root, "hover-middle").colorBeside(root, bounds("Hover me"))
        mainClock.advanceTimeBy(200)
        val hovered = saveScreenshot(root, "hover-end").colorBeside(root, bounds("Hover me"))

        assertTrue(rest != hovered, "Expected the hover theme to change the button's colour")
        for (channel in listOf(java.awt.Color::getRed, java.awt.Color::getGreen, java.awt.Color::getBlue)) {
            val (a, b) = listOf(channel(rest), channel(hovered)).sorted()
            assertTrue(channel(middle) in a..b, "Expected $middle between $rest and $hovered")
        }
        assertTrue(middle != rest && middle != hovered, "Expected $middle to be part way from $rest to $hovered")
    }

    @Test
    fun swapViewCrossFades() = runKiteUiTest {
        mainClock.autoAdvance = false
        lateinit var swapView: SwapView
        val root = showKiteUi {
            col {
                swapView {
                    swapView = this
                    swap(ScreenTransition.Fade) { themed(swatch(true)).frame { text("Old") } }
                }
            }
        }
        runOnUiThread { swapView.swap(ScreenTransition.Fade) { themed(swatch(false)).frame { text("New") } } }
        mainClock.advanceTimeBy(125)
        onNodeWithText("Old", useUnmergedTree = true).assertExists()
        onNodeWithText("New", useUnmergedTree = true).assertExists()
        val middle = saveScreenshot(root, "swap-middle").colorBeside(root, bounds("New"))
        assertTrue(middle.red in 30..225 && middle.blue in 30..225, "Expected the views blended, got $middle")

        mainClock.advanceTimeBy(200)
        waitForIdle()
        onNodeWithText("Old", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(255, saveScreenshot(root, "swap-end").colorBeside(root, bounds("New")).blue)
    }

    @Test
    fun swapViewSlides() = runKiteUiTest {
        mainClock.autoAdvance = false
        lateinit var swapView: SwapView
        val root = showKiteUi {
            col {
                swapView {
                    swapView = this
                    swap(ScreenTransition.Push) { text("Old") }
                }
            }
        }
        val width = onRoot().getUnclippedBoundsInRoot().width
        runOnUiThread { swapView.swap(ScreenTransition.Push) { text("New") } }
        mainClock.advanceTimeBy(125)
        saveScreenshot(root, "push-middle")
        val old = bounds("Old").left
        val new = bounds("New").left
        assertTrue(old < 0.dp && old > -width, "Expected the old view part way out to the left, got $old")
        assertTrue(new > 0.dp && new < width, "Expected the new view part way in from the right, got $new")
        mainClock.advanceTimeBy(200)
        waitForIdle()
        assertEquals(0f, bounds("New").left.value, 0.5f)
    }

    @Test
    fun shownWhenCollapsesSoSiblingsMoveSmoothly() = runKiteUiTest {
        mainClock.autoAdvance = false
        val shown = Signal(true)
        val root = showKiteUi {
            col {
                text("Above")
                shownWhen { shown() }.sizeConstraints(height = 100.px).text("Middle")
                text("Below")
            }
        }
        val open = bounds("Below").top
        runOnUiThread { shown.value = false }
        mainClock.advanceTimeBy(125)
        val middle = bounds("Below").top
        saveScreenshot(root, "collapse-middle")
        mainClock.advanceTimeBy(200)
        waitForIdle()
        val closed = bounds("Below").top
        onNodeWithText("Middle", useUnmergedTree = true).assertDoesNotExist()

        assertTrue(closed < open - 90.dp, "Expected Below to move up by Middle's height and a gap, from $open to $closed")
        assertTrue(middle > closed + 10.dp && middle < open - 10.dp, "Expected Below part way, between $open and $closed, got $middle")

        // And back, from where it is.
        runOnUiThread { shown.value = true }
        mainClock.advanceTimeBy(125)
        val reopening = bounds("Below").top
        assertTrue(reopening > closed + 10.dp && reopening < open - 10.dp, "Expected Below part way back, got $reopening")
        mainClock.advanceTimeBy(200)
        waitForIdle()
        assertEquals(open.value, bounds("Below").top.value, 0.5f)
    }

    @Test
    fun dynamicWeightAnimatesFromNaturalSize() = runKiteUiTest {
        mainClock.autoAdvance = false
        val first = Signal(true)
        val root = showKiteUi {
            sizeConstraints(height = 400.px).col {
                dynamicWeight { if (first()) 1f else 0f }.text("First")
                dynamicWeight { if (first()) 0f else 1f }.text("Second")
            }
        }
        val start = bounds("Second").top
        runOnUiThread { first.value = false }
        mainClock.advanceTimeBy(125)
        val middle = bounds("Second").top
        saveScreenshot(root, "weight-middle")
        mainClock.advanceTimeBy(200)
        waitForIdle()
        val end = bounds("Second").top
        assertTrue(end < start - 100.dp, "Expected Second to take over the space, from $start to $end")
        assertTrue(middle < start - 20.dp && middle > end + 20.dp, "Expected Second part way, between $start and $end, got $middle")
    }

    @Test
    fun dialogsFadeOutBeforeGoing() = runKiteUiTest {
        mainClock.autoAdvance = false
        lateinit var close: () -> Unit
        showKiteUi {
            frame {
                context.overlayFrame = this
                text("Page")
            }
        }
        runOnUiThread {
            KiteUiRoot.current!!.context.overlay(transition = ScreenTransitions.Fade) { remove ->
                close = remove
                centered.card.text("Dialog")
            }
        }
        mainClock.advanceTimeBy(400)
        onNodeWithText("Dialog").assertExists()
        runOnUiThread { close() }
        mainClock.advanceTimeBy(125)
        onNodeWithText("Dialog").assertExists()
        mainClock.advanceTimeBy(200)
        waitForIdle()
        onNodeWithText("Dialog").assertDoesNotExist()
    }

    // There and back in 2s, as on the web.
    @Test
    fun fadingColorsFade() = runKiteUiTest {
        mainClock.autoAdvance = false
        val root = showKiteUi {
            col {
                themed(ThemeDerivation { it.copy(id = "fading", background = FadingColor(Color.red, Color.blue)).withBack }).frame { text("Loading") }
            }
        }
        mainClock.advanceTimeBy(1000)
        val alternate = saveScreenshot(root, "fading-alternate").colorBeside(root, bounds("Loading"))
        mainClock.advanceTimeBy(1000)
        val base = saveScreenshot(root, "fading-base").colorBeside(root, bounds("Loading"))
        assertTrue(alternate.blue > 200 && alternate.red < 55, "Expected nearly blue after 1s, got $alternate")
        assertTrue(base.red > 200 && base.blue < 55, "Expected nearly red after 2s, got $base")
    }
}

