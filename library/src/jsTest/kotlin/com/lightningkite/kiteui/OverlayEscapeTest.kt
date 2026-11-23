@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.direct.Button
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OverlayEscapeTest {
    private class Mounted {
        lateinit var context: ElementContext
        lateinit var openerButton: Button
        val opener: HTMLElement get() = openerButton.native.element as HTMLElement

        init {
            root(Theme(id = "unitTest")) {
                frame {
                    this@Mounted.context = context
                    context.overlayFrame = this
                    button {
                        text("opener")
                        openerButton = this
                    }
                }
            }
        }
    }

    private fun shown(text: String) = document.body!!.textContent!!.contains(text)

    // Sent to whatever has focus, like a real key press.
    private fun pressEscape() {
        val target = document.activeElement ?: document.body!!
        target.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Escape", code = "Escape", bubbles = true)))
    }

    @Test
    fun escapeClosesTheTopmostDismissableDialogOnly() = GlobalScope.promise {
        val mounted = Mounted()
        mounted.context.dialog { text("first-dialog") }
        // Opened once focus is inside the first, as when a button in a dialog opens another.
        delay(300)
        mounted.context.dialog { text("second-dialog") }
        delay(300)

        pressEscape()
        delay(600)
        assertTrue(shown("first-dialog"))
        assertFalse(shown("second-dialog"))

        pressEscape()
        delay(600)
        assertFalse(shown("first-dialog"))
    }

    @Test
    fun escapeLeavesANonDismissableDialogOpen() = GlobalScope.promise {
        val mounted = Mounted()
        mounted.context.dialog(dismissable = false) { text("locked-dialog") }
        delay(300)

        pressEscape()
        delay(600)
        assertTrue(shown("locked-dialog"))
    }

    @Test
    fun focusMovesIntoTheDialogAndBackToTheOpener() = GlobalScope.promise {
        val mounted = Mounted()
        mounted.opener.focus()
        mounted.context.dialog { button { text("inside") } }
        delay(300)
        assertEquals("inside", (document.activeElement as HTMLElement).textContent)

        pressEscape()
        delay(600)
        assertEquals(mounted.opener, document.activeElement)
    }
}
