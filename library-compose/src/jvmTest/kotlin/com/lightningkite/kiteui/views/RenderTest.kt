package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class RenderTest {
    @Test
    fun loginLikePage() = runKiteUiTest {
        val name = Signal("")
        val greeting = Signal("Nobody greeted yet")
        val root = showKiteUi {
            col {
                text("Sign in")
                card.col {
                    text("Name")
                    textInput {
                        hint = "Your name"
                        content bind name
                    }
                    important.button {
                        text("Greet")
                        onClick { greeting.value = "Hello, ${name.value}!" }
                    }
                }
                text { ::content { greeting() } }
            }
        }

        onNodeWithText("Nobody greeted yet").assertExists()
        onNode(hasSetTextAction()).performTextInput("Ada")
        waitForIdle()
        check(name.value == "Ada") { "Typing didn't reach the bound signal: '${name.value}'" }
        onNodeWithText("Greet").performClick()
        waitForIdle()
        onNodeWithText("Hello, Ada!").assertExists()

        saveScreenshot(root, "login")
    }

    // The web gives a stack's children a z-index, which paints them over the stack's outline; a row's children have
    // none, so its outline paints over them.
    @Test
    fun aStacksChildrenCoverItsOutlineButARowsDoNot() = runKiteUiTest {
        lateinit var stack: Frame
        lateinit var row: RowOrCol
        val root = showKiteUi {
            col {
                stack = sizeConstraints(width = 40.px, height = 40.px).themed(Outlined).frame {
                    themed(Filled).frame { }
                }
                row = sizeConstraints(width = 40.px, height = 40.px).themed(Outlined).row {
                    expanding.themed(Filled).frame { }
                }
            }
        }
        val image = saveScreenshot(root, "outline-under-stack-children")
        fun leftEdge(view: NativeElement): Int {
            val bounds = runOnUiThread { view.screenRectangle()!! }
            return image.getRGB(with(density) { (bounds.left + 1).dp.roundToPx() }, with(density) { ((bounds.top + bounds.bottom) / 2).dp.roundToPx() })
        }
        assertEquals(0xFFFF0000.toInt(), leftEdge(stack), "a stack's child should cover its outline")
        assertEquals(0xFF0000FF.toInt(), leftEdge(row), "a row's outline should cover its child")
    }

    @Test
    fun elevationCastsTheWebsBoxShadow() = runKiteUiTest {
        lateinit var box: Frame
        val root = showKiteUi {
            frame {
                themed(WhitePage).frame {
                    box = centered.sizeConstraints(width = 200.px, height = 100.px).themed(Raised).frame { }
                }
            }
        }
        val image = saveScreenshot(root, "elevation-shadow")
        val bounds = runOnUiThread { box.screenRectangle()!! }
        fun grey(x: Double, y: Double): Int {
            val rgb = image.getRGB(with(density) { x.dp.roundToPx() }, with(density) { y.dp.roundToPx() })
            return minOf(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
        }
        val centerX = (bounds.left + bounds.right) / 2
        val centerY = (bounds.top + bounds.bottom) / 2
        val below = List(20) { grey(centerX, bounds.bottom + it) }
        val right = List(20) { grey(bounds.right + it, centerY) }
        // Chrome's, for the same box with KiteUiCss's box-shadow.
        val chromeBelow = listOf(165, 179, 204, 221, 232, 239, 246, 251, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255)
        val chromeRight = listOf(227, 235, 242, 247, 250, 252, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255)
        fun close(a: List<Int>, b: List<Int>) = a.zip(b).all { (x, y) -> abs(x - y) <= 4 }
        assertTrue(close(below, chromeBelow), "below: $below, Chrome's: $chromeBelow")
        assertTrue(close(right, chromeRight), "right: $right, Chrome's: $chromeRight")
    }
}

private object WhitePage : Semantic("white-page") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(background = Color.white, outlineWidth = 0.px, elevation = 0.px).withBackNoPadding
}

private object Raised : Semantic("raised-box") {
    override fun default(theme: Theme): ThemeAndBack =
        theme.alter(background = Color.white, outlineWidth = 0.px, elevation = 4.px, cornerRadii = CornerRadii.Fixed(8.px)).withBackNoPadding
}

private object Outlined : Semantic("outlined") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(outline = Color.blue, outlineWidth = 4.px).withBackNoPadding
}

private object Filled : Semantic("filled") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(background = Color.red, outlineWidth = 0.px).withBackNoPadding
}
