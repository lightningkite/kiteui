package com.lightningkite.kiteui.testing

import kotlinx.coroutines.runBlocking

/**
 * Runs a UI test against a remote app connected to the AI driver daemon.
 * Same [UiTestScope] API as local tests — tests work identically local or remote.
 *
 * @param startRoute If provided, resets the navigator stack to this route before running the test,
 *   giving every test a clean known starting state regardless of what prior tests did.
 */
fun remoteUiTest(
    appId: String,
    host: String = "127.0.0.1",
    port: Int = 7474,
    startRoute: String? = null,
    block: suspend UiTestScope.() -> Unit,
) {
    val backend = RemoteUiTestBackend(appId, host, port)
    val scope = UiTestScope(backend)
    runBlocking {
        if (startRoute != null) scope.reset(startRoute)
        scope.block()
    }
}
