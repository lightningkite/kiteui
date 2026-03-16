package com.lightningkite.kiteui.testing

import kotlinx.coroutines.runBlocking

/**
 * Runs a UI test against a remote app connected to the AI driver daemon.
 * Same [UiTestScope] API as local tests — tests work identically local or remote.
 */
fun remoteUiTest(
    appId: String,
    host: String = "127.0.0.1",
    port: Int = 7474,
    block: suspend UiTestScope.() -> Unit,
) {
    val backend = RemoteUiTestBackend(appId, host, port)
    val scope = UiTestScope(backend)
    runBlocking { scope.block() }
}
