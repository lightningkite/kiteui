package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

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

    @Suppress("OPT_IN_USAGE")
    return GlobalScope.promise {
        try {
            scope.block()
        } finally {
            harness.cleanup()
        }
    }.unsafeCast<Unit>()
}
