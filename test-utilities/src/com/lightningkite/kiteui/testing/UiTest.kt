package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.views.ViewWriter

/**
 * Runs a UI test with a view tree built by [content] and a test body using [UiTestScope].
 * Platform-specific implementations set up the rendering context and dispatch loop.
 *
 * @param mockExternalServices Optional mock for file pickers, geolocation, etc.
 *   Installed on the view context before [content] runs, so any code calling
 *   `context.requestFile(...)` will hit the mock's queued responses.
 */
expect fun uiTest(
    mockExternalServices: MockExternalServices? = null,
    content: ViewWriter.() -> Unit = {},
    block: suspend UiTestScope.() -> Unit,
)
