package com.lightningkite.kiteui

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.exceptions.installStandardHandlers
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a page leaves behind once its test is over: a later test must not be talking to it. */
class UiTestTeardownTest {
    @Test
    fun aPageStopsListeningWhenItsTestEnds() {
        val session = Signal(0)
        uiTest(content = {
            text {
                ::content {
                    session()
                    // Still working when the test ends, the way a request to the server can be.
                    async("load") { withContext(Dispatchers.IO) { Thread.sleep(200) } }
                    "loaded"
                }
            }
        }) {}
        assertEquals(0, session.listenerCount, "The finished test's page still reruns when the session changes")
    }

    @OptIn(OverrideOnly::class)
    @Test
    fun anElementWrittenIntoAShutDownContainerIsShutDownToo() {
        val session = Signal(0)
        lateinit var root: Frame
        uiTest(content = { root = this as Frame }) {
            root.onShutdown()
            // A reactive in the dead page firing mid-teardown can still build children.
            with(root) { text { ::content { session().toString() } } }
            assertEquals(0, session.listenerCount)
        }
    }

    @Test
    fun eachElementTreeHasItsOwnErrorDialog() {
        lateinit var first: ElementContext
        uiTest(content = {
            context.exceptionHandlers.installStandardHandlers()
            first = context
        }) {
            first.handleException(PlainTextException("First"))
            waitForText("First")

            val second = ElementContext("/")
            val secondRoot = Frame(second)
            second.overlayFrame = secondRoot
            second.exceptionHandlers.installStandardHandlers()
            @OptIn(OverrideOnly::class)
            secondRoot.onStartup()
            second.handleException(PlainTextException("Second"))
            val html = buildString { secondRoot.native.render(this) }
            assertTrue("Second" in html, "The second app's error was swallowed by the first app's dialog:\n$html")
        }
    }
}
