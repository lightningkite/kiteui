package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

actual fun uiTest(
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(Dispatchers.Unconfined)

    try {
        val context = ElementContext("/")
        val root = Frame(context)
        content(root)
        root.postSetup()

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
