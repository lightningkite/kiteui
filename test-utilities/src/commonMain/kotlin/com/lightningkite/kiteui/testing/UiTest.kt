package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.ViewWriter

/**
 * Runs a UI test with a view tree built by [content] and a test body using [UiTestScope].
 * Platform-specific implementations set up the rendering context and dispatch loop.
 */
expect fun uiTest(
    content: ViewWriter.() -> Unit = {},
    block: suspend UiTestScope.() -> Unit,
)
