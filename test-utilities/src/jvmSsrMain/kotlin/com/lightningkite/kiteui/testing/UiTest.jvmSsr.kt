// by Claude - JVM SSR actual for uiTest using SsrContext.
// SSR is the fastest test target: pure JVM, no emulator/browser/simulator.
// CommonHtml view overrides work via direct state manipulation, so
// dispatchAction works on SSR view trees kept alive after rendering.
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
actual fun uiTest(
    config: UiTestConfig,
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit
) {
    Dispatchers.setMain(Dispatchers.Unconfined)
    try {
        val ssrContext = SsrContext("/")
        ssrContext.theme = config.theme ?: Theme(id = "test")
        // by Claude - set pageNavigator and externalServices on the ViewWriter
        ssrContext.render {
            config.navigator?.let { pageNavigator = it }
            config.externalServices?.let { externalServices = it }
            content()
        }
        val root = ssrContext.rootFrame
            ?: error("SsrContext.render() produced no root frame")
        val scope = UiTestScope(
            root = root,
            navigator = config.navigator,
            // SSR uses Dispatchers.Unconfined — reactive updates are synchronous
            idle = { }
        )
        runTest { scope.block() }
    } finally {
        Dispatchers.resetMain()
    }
}
