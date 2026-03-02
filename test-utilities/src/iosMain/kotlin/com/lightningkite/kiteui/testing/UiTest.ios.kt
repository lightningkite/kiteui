// by Claude - iOS actual for uiTest using native TestHarness
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.coroutines.test.runTest

actual fun uiTest(
    config: UiTestConfig,
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit
) {
    withTestHarness { harness ->
        // by Claude - wrap content to inject externalServices mock before user content runs
        val wrappedContent: ViewWriter.() -> Unit = {
            config.externalServices?.let { externalServices = it }
            content()
        }
        val root = harness.render(config.theme ?: Theme(id = "test"), wrappedContent)
        val scope = UiTestScope(
            root = root,
            navigator = config.navigator,
            idle = { harness.waitUntilIdle() }
        )
        runTest { scope.block() }
    }
}
