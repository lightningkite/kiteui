package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

/**
 * JVM SSR implementation of TestHarness.
 *
 * Note: Currently unsupported - JVM SSR doesn't have a full view hierarchy.
 * Tests will be skipped on this platform.
 */
actual class TestHarness {
    // JVM SSR doesn't have full UI rendering support in tests
    actual val supported: Boolean = false

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): RView {
        throw UnsupportedOperationException("JVM SSR tests are not yet supported")
    }

    actual fun screenshot(name: String): ByteArray? = null

    actual fun screenshotView(view: RView, name: String): ByteArray? = null

    actual fun cleanup() {
        // No-op
    }
}
