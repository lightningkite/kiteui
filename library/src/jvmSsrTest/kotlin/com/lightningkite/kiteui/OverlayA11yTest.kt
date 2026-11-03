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
 * Verifies that modal overlays (ElementContext.dialog) are exposed to assistive technology as
 * dialogs, while non-modal overlays (ElementContext.toast) are not.
 */
class OverlayA11yTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun modalDialogGetsDialogRole() {
        val context = ElementContext("/")
        val frame = Frame(context)
        context.overlayFrame = frame

        context.dialog { text("hi") }

        val html = buildString { frame.native.render(this) }
        assertTrue(html.contains("role='dialog'"), "modal overlay should have role='dialog'")
        assertTrue(html.contains("aria-modal='true'"), "modal overlay should have aria-modal='true'")
    }

    @Test
    fun toastOverlayIsNotDialog() {
        val context = ElementContext("/")
        val frame = Frame(context)
        context.overlayFrame = frame

        context.toast("msg")

        val html = buildString { frame.native.render(this) }
        assertFalse(html.contains("role='dialog'"), "non-modal toast must not have role='dialog'")
        assertFalse(html.contains("aria-modal='true'"), "non-modal toast must not have aria-modal='true'")
    }
}
