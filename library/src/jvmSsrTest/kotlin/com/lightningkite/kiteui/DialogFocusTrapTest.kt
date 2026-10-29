package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.l2.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that opening a modal dialog moves focus into it and traps it there by making the rest
 * of the UI inert, while non-modal overlays leave the rest of the UI alone.
 */
class DialogFocusTrapTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun modalDialogMakesBackgroundInert() {
        val context = ElementContext("/")
        val frame = Frame(context)
        context.overlayFrame = frame
        frame.text("background")

        context.dialog { text("hi") }

        val html = buildString { frame.native.render(this) }
        assertTrue(html.contains("inert=''"), "background content should be inert while a modal dialog is open")
    }

    @Test
    fun modalDialogReceivesFocus() {
        val context = ElementContext("/")
        val frame = Frame(context)
        context.overlayFrame = frame
        frame.text("background")

        context.dialog { text("hi") }

        val html = buildString { frame.native.render(this) }
        assertTrue(html.contains("tabindex='-1'"), "dialog container should be focusable as a focus fallback")
        assertTrue(html.contains("autofocus='true'"), "focus should be moved into the dialog")
    }

    @Test
    fun toastDoesNotTrapFocus() {
        val context = ElementContext("/")
        val frame = Frame(context)
        context.overlayFrame = frame
        frame.text("background")

        context.toast("msg")

        val html = buildString { frame.native.render(this) }
        assertFalse(html.contains("inert=''"), "a non-modal toast must not make the background inert")
        assertFalse(html.contains("aria-modal='true'"), "a non-modal toast must not be modal")
    }
}
