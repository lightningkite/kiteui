package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.root
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame

/**
 * JavaScript/Web implementation of TestHarness.
 *
 * Uses the `root(Theme) {}` pattern which is the standard way to
 * initialize KiteUI on the web platform.
 */
actual class TestHarness {
    actual val supported: Boolean = true
    private var rootView: RView? = null

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): RView {
        lateinit var capturedRoot: RView
        root(theme) {
            frame {
                content()
            }.also { capturedRoot = it }
        }
        rootView = capturedRoot
        return capturedRoot
    }

    actual fun screenshot(name: String): ByteArray? {
        // TODO: Implement JS screenshot using canvas
        // For now, return null (screenshots not yet supported on JS)
        return null
    }

    actual fun screenshotView(view: RView, name: String): ByteArray? {
        // TODO: Implement JS screenshot using canvas
        // For now, return null (screenshots not yet supported on JS)
        return null
    }

    actual fun cleanup() {
        rootView?.shutdown()
        rootView = null
    }
}
