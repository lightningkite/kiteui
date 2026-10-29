package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.exceptions.installStandardHandlers
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ErrorDialogTest {
    @Test
    fun errorsShareOneDialog() = runKiteUiTest {
        lateinit var page: ElementContext
        val root = showKiteUi {
            frame {
                context.overlayFrame = this
                context.exceptionHandlers.installStandardHandlers()
                page = context
                text("Page")
            }
        }
        fun ComposeUiTest.count(text: String) = onAllNodesWithText(text).fetchSemanticsNodes().size
        fun raise(vararg messages: String) = runBlocking(Dispatchers.Main) {
            for (m in messages) page.handleException(PlainTextException(m))
        }

        raise("First", "Second", "First")
        waitUntil { count("First") == 1 && count("Second") == 1 }
        // One dialog, so one Close; the repeated First isn't shown twice.
        assertEquals(1, count("Close"))
        waitForIdle()
        assertEquals(1, count("First"))
        saveScreenshot(root, "error-dialog-combined")

        onNodeWithText("Close").performClick()
        waitUntil { count("First") == 0 && count("Second") == 0 }

        // Once it's closed, the next error opens a new dialog.
        raise("Third")
        waitUntil { count("Third") == 1 }
        assertEquals(1, count("Close"))
    }
}
