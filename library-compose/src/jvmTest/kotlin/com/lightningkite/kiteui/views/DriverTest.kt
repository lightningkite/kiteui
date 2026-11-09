package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.appBase
import com.lightningkite.kiteui.views.l2.dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DriverTest {
    private fun ComposeUiTest.showApp(content: ContainerElement.() -> Unit): Pair<KiteUiRoot, PageNavigator> {
        val home = Page.Direct("Home") { text("Home page") }
        val navigator = PageNavigator { Routes(parsers = listOf(), renderers = mapOf(), fallback = home) }
        val root = showKiteUi { appBase(navigator) { content() } }
        return root to navigator
    }

    private fun ComposeUiTest.openDialog(frame: ContainerElement) {
        runBlocking(Dispatchers.Main) { frame.context.dialog { text("No buttons here") } }
        waitForIdle()
        onNodeWithText("No buttons here").assertExists()
    }

    @Test
    fun aMouseClickOnADialogsBackgroundClosesIt() = runKiteUiTest {
        lateinit var frame: ContainerElement
        showApp { frame = this }
        openDialog(frame)
        onRoot().performMouseInput { click(Offset(5f, 5f)) }
        waitUntilDoesNotExist(hasText("No buttons here"))
    }

    @Test
    fun theDriverDismissesADialogThroughItsBackground() = runKiteUiTest {
        lateinit var frame: ContainerElement
        val (root, navigator) = showApp { frame = this }
        openDialog(frame)
        val reply = runBlocking(Dispatchers.Main) { handleCommand("dialog-bg\tdismiss", root.root, navigator) }
        assertEquals("OK", reply)
        waitUntilDoesNotExist(hasText("No buttons here"))
    }

    @Test
    fun theDriverDismissesAMenuLikeAClickOutside() = runKiteUiTest {
        val (root, navigator) = showApp {
            menuButton {
                requireClick = true
                opensMenu { text("First item") }
                text("Menu")
            }
        }
        onNodeWithText("Menu").performClick()
        waitForIdle()
        onNodeWithText("First item").assertExists()

        val found = runBlocking(Dispatchers.Main) { handleCommand("root\tfind\tdismiss", root.root, navigator) }
        val path = found.substringBefore(":")
        assertEquals("OK", runBlocking(Dispatchers.Main) { handleCommand("$path\tdismiss", root.root, navigator) })
        waitUntilDoesNotExist(hasText("First item"))
    }

    @Test
    fun theDriversBackClosesTheTopDialogBeforeThePage() = runKiteUiTest {
        lateinit var frame: ContainerElement
        val (root, navigator) = showApp { frame = this }
        runBlocking(Dispatchers.Main) { navigator.navigate(Page.Direct("Second") { text("Second page") }) }
        openDialog(frame)

        runBlocking(Dispatchers.Main) { handleCommand("back", root.root, navigator) }
        waitUntilDoesNotExist(hasText("No buttons here"))
        assertEquals(2, navigator.stack.value.size)

        runBlocking(Dispatchers.Main) { handleCommand("back", root.root, navigator) }
        assertEquals(1, navigator.stack.value.size)
    }
}
