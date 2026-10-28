package com.lightningkite.kiteui.views

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.appBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Building KeyEvents is internal Compose API; the window builds them in the app.
@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class)
class KeyboardTest {
    private fun keyDown(key: Key, alt: Boolean = false, ctrl: Boolean = false) =
        KeyEvent(key, KeyEventType.KeyDown, isAltPressed = alt, isCtrlPressed = ctrl)

    private fun ComposeUiTest.showApp(content: ContainerElement.() -> Unit): Pair<KiteUiRoot, PageNavigator> {
        val home = Page.Direct("Home") { text("Home page") }
        val navigator = PageNavigator { Routes(parsers = listOf(), renderers = mapOf(), fallback = home) }
        val root = showKiteUi { appBase(navigator) { content() } }
        return root to navigator
    }

    @Test
    fun keyCodesAreComposeKeys() {
        assertEquals(Key.A.keyCode, KeyCodes.letter('a'))
        assertEquals(Key.Z.keyCode, KeyCodes.letter('Z'))
        assertEquals(Key.Seven.keyCode, KeyCodes.num(7))
        assertEquals(Key.NumPad0.keyCode, KeyCodes.numpad(0))
        assertEquals(Key.DirectionLeft.keyCode, KeyCodes.left)
        assertEquals(Key.Escape.keyCode, KeyCodes.escape)
    }

    @Test
    fun universalKeyboardHandlersGetKeysWithModifiers() = runKiteUiTest {
        val (root, _) = showApp { text("Content") }
        val seen = ArrayList<KeyCodeWithModifiers>()
        val stop = runBlocking(Dispatchers.Main) {
            AppState.onUniversalKeyboard {
                seen.add(it)
                it == keyCode { Ctrl + it.letter('s') }
            }
        }
        runBlocking(Dispatchers.Main) {
            assertTrue(root.onKeyEvent(keyDown(Key.S, ctrl = true)), "a handled key is consumed")
            assertFalse(root.onKeyEvent(keyDown(Key.Q)), "an unhandled key is not")
            assertFalse(root.onKeyEvent(KeyEvent(Key.S, KeyEventType.KeyUp, isCtrlPressed = true)), "key releases aren't passed on")
        }
        assertEquals(listOf(KeyCodeWithModifiers(KeyCodes.letter('s'), ctrl = true), KeyCodeWithModifiers(KeyCodes.letter('q'))), seen)

        runBlocking(Dispatchers.Main) {
            stop()
            root.onKeyEvent(keyDown(Key.S, ctrl = true))
        }
        assertEquals(2, seen.size, "a stopped handler hears nothing")
    }

    @Test
    fun altLeftClosesTheTopDialogThenGoesBack() = runKiteUiTest {
        lateinit var frame: ContainerElement
        val (root, navigator) = showApp { frame = this }
        runBlocking(Dispatchers.Main) {
            navigator.navigate(Page.Direct("Second") { text("Second page") })
            frame.context.overlay(navClosable = true) { frame { text("First dialog") } }
            frame.context.overlay(navClosable = true) { frame { text("Second dialog") } }
        }
        waitForIdle()
        onNodeWithText("Second dialog").assertExists()

        runBlocking(Dispatchers.Main) { assertTrue(root.onKeyEvent(keyDown(Key.DirectionLeft, alt = true))) }
        waitUntilDoesNotExist(hasText("Second dialog"))
        onNodeWithText("First dialog").assertExists()
        assertEquals(2, navigator.stack.value.size)

        runBlocking(Dispatchers.Main) { root.onKeyEvent(keyDown(Key.DirectionLeft, alt = true)) }
        waitUntilDoesNotExist(hasText("First dialog"))
        assertEquals(2, navigator.stack.value.size)

        runBlocking(Dispatchers.Main) { root.onKeyEvent(keyDown(Key.DirectionLeft, alt = true)) }
        assertEquals(1, navigator.stack.value.size)
    }

    @Test
    fun mouseBackButtonGoesBack() = runKiteUiTest {
        val (root, navigator) = showApp { text("Content") }
        runBlocking(Dispatchers.Main) { navigator.navigate(Page.Direct("Second") { text("Second page") }) }
        assertEquals(2, navigator.stack.value.size)
        onRoot().performMouseInput { press(MouseButton(3)); release(MouseButton(3)) }
        waitForIdle()
        assertEquals(1, navigator.stack.value.size)
    }

    @Test
    fun escapeClosesPopovers() = runKiteUiTest {
        lateinit var anchor: Element
        val (root, _) = showApp { anchor = text("Anchor") }
        runBlocking(Dispatchers.Main) { anchor.openPopover(PopoverPreferredDirection.belowRight) { text("Popover content") } }
        waitForIdle()
        onNodeWithText("Popover content").assertExists()
        runBlocking(Dispatchers.Main) { root.onKeyEvent(keyDown(Key.Escape)) }
        waitUntilDoesNotExist(hasText("Popover content"))
    }

    // As on the web, Ctrl, Meta or Shift with Enter or Space runs a button's secondary action.
    @Test
    fun modifiedEnterOrSpaceRunsTheSecondaryAction() = runKiteUiTest {
        var primary = 0
        var secondary = 0
        showApp {
            button {
                text("Open")
                // Without the usual cap on how often an action runs, so presses in quick succession all count.
                action = Action("Open", frequencyCap = null) { primary++ }
                secondaryAction = Action("Open in new tab", frequencyCap = null) { secondary++ }
            }
        }
        val button = onNodeWithText("Open")
        button.requestFocus()
        button.performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 2_000) { primary == 1 }
        button.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) } }
        button.performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.Spacebar) } }
        button.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitUntil(timeoutMillis = 2_000) { secondary == 3 }
        assertEquals(1, primary)
    }

    @Test
    fun focusedCanvasGetsKeys() = runKiteUiTest {
        val events = ArrayList<String>()
        val (root, _) = showApp {
            canvas {
                delegate = object : CanvasDelegate() {
                    override fun onKeyDown(key: KeyCode): Boolean = events.add("down $key")
                    override fun onKeyUp(key: KeyCode): Boolean = events.add("up $key")
                }
            }
        }
        onNode(isFocusable()).performClick()
        waitForIdle()
        onNode(isFocused()).performKeyInput { pressKey(Key.A) }
        waitForIdle()
        assertEquals(listOf("down ${KeyCodes.letter('a')}", "up ${KeyCodes.letter('a')}"), events)
    }
}
