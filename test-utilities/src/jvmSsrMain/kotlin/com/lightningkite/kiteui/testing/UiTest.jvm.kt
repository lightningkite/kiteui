package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

actual fun uiTest(
    mockExternalServices: MockExternalServices?,
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(Dispatchers.Unconfined)

    try {
        val context = RContext("/")
        val root = Frame(context)
        root.overlayFrame = root
        if (mockExternalServices != null) {
            context.addons[ViewWriter::externalServices.name] = mockExternalServices
        }
        content(root)
        root.onStartup()

        val backend = LocalUiTestBackend(
            root = { root },
            navigator = { null },
        )
        val scope = UiTestScope(backend)

        runBlocking(Dispatchers.Unconfined) {
            scope.block()
        }
    } finally {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        Dispatchers.resetMain()
    }
}
