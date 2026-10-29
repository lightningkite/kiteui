package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.exceptions.installStandardHandlers
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ErrorDialogTest {
    @Test
    fun errorDialogsShowOneAtATime() = runKiteUiTest {
        lateinit var page: ElementContext
        showKiteUi {
            frame {
                context.overlayFrame = this
                context.exceptionHandlers.installStandardHandlers()
                page = context
                text("Page")
            }
        }
        runBlocking(Dispatchers.Main) {
            page.handleException(PlainTextException("First"))
            page.handleException(PlainTextException("Second"))
            page.handleException(PlainTextException("First"))
        }
        fun ComposeUiTest.shown(text: String) = onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

        waitUntil { shown("First") }
        onNodeWithText("Second").assertDoesNotExist()

        onNodeWithText("Close").performClick()
        waitUntil { shown("Second") && !shown("First") }

        // The repeated "First" was dropped while it was waiting.
        onNodeWithText("Close").performClick()
        waitUntil { !shown("Second") }
        waitForIdle()
        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Close").assertDoesNotExist()
    }
}
