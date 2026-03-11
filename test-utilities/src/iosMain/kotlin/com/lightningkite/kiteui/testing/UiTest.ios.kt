package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

actual fun uiTest(
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    val harness = TestHarness()
    val root = harness.render { content() }

    val backend = LocalUiTestBackend(
        root = { root },
        navigator = { null },
    )
    val scope = UiTestScope(backend)

    runBlocking(Dispatchers.Unconfined) {
        try {
            scope.block()
        } finally {
            harness.cleanup()
        }
    }
}
