package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.RouteRendered
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.l2.appBase
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.core.Constant
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
    fun theDriversLinkClickRunsTheLinksActionsAsARealClickDoes() = runKiteUiTest {
        val ran = ArrayList<String>()
        val (root, navigator) = showApp {
            link {
                to = { Page.Direct("Second") { text("Second page") } }
                onClick { ran += "click" }
                onNavigate { ran += "navigate" }
                text("Go")
            }
        }
        val path = runBlocking(Dispatchers.Main) { handleCommand("root\tfindClickable\tGo", root.root, navigator) }.substringBefore(":")
        assertEquals("OK", runBlocking(Dispatchers.Main) { handleCommand("$path\tclick", root.root, navigator) })
        waitForIdle()
        assertEquals(listOf("click", "navigate"), ran)
        assertEquals(2, navigator.stack.value.size)
    }

    @Test
    fun theDriverClicksABottomSheetsHandleToToggleIt() = runKiteUiTest {
        lateinit var frame: CoordinatorFrame
        lateinit var control: BottomSheetControl
        val (root, navigator) = showApp { frame = coordinatorFrame { text("Page behind") } }
        runOnUiThread {
            frame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) {
                control = it
                col { coordinatorDragHandle(); text("Sheet content") }
            }
        }
        waitForIdle()
        val path = runBlocking(Dispatchers.Main) { handleCommand("root\tfind\tCoordinatorDragHandle", root.root, navigator) }.substringBefore(":")
        assertEquals("OK", runBlocking(Dispatchers.Main) { handleCommand("$path\tclick", root.root, navigator) })
        assertEquals(BottomSheetState.EXPANDED, control.state.state.getOrNull())
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

    @Test
    fun eachSnapshotLineStartsWithAPathToItsView() = runKiteUiTest {
        val (root, navigator) = showApp {
            col {
                row { button { text("Unnamed"); onClick { } } }
                col { debugName = "twin"; text("First twin") }
                col { col { debugName = "twin"; button { text("Second twin"); onClick { } } } }
                sizeConstraints(height = 100.px).recyclerView {
                    children(Constant(listOf(1, 2)), id = { it }) { item -> button { text { ::content { "Item ${item()}" } }; onClick { } } }
                }
                menuButton {
                    requireClick = true
                    opensMenu { button { text("Menu item"); onClick { } } }
                    text("Menu")
                }
            }
        }
        fun command(command: String) = runBlocking(Dispatchers.Main) { handleCommand(command, root.root, navigator) }
        val menu = command("${command("root\tfindClickable\tMenu").substringBefore(":")}\tclick")
        waitForIdle()
        val lines = (command("root\tsnapshot") + command("root\tsnapshot\t--interactive") + menu).lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            assertEquals(line, command("${line.substringBefore(": ")}\tsnapshot").lines().first())
        }
    }

    // As the web's address bar, which a page without a route (a full-screen image, say) leaves as it was.
    @Test
    fun theDriversUrlIsTheTopmostPageWithARoute() {
        val navigator = PageNavigator {
            Routes(parsers = listOf(), renderers = mapOf(Routed::class to { RouteRendered(UrlLikePath(listOf("routed"), mapOf()), listOf()) }))
        }
        navigator.navigate(Routed)
        navigator.navigate(Page.Direct("No route") { })
        assertEquals("/routed", runBlocking { handleCommand("url", null, navigator) })
    }

    // Queued for a multi-file pick, files must not be left over for the next, unrelated request.
    @Test
    fun aMultiFileRequestTakesEveryQueuedFile() {
        val mock = MockExternalServices()
        val files = listOf("a.png", "b.png").map { createFileReferenceFromBytes(ByteArray(1), "image/png", it) }
        mock.pendingFileResponses.addAll(files)
        assertEquals(files, runBlocking { mock.requestFiles(listOf("image/*")) })
        assertEquals(0, mock.pendingFileResponses.size)
    }
}

private object Routed : Page {
    override fun ElementWriter.CanAddTheme.render() {}
}
