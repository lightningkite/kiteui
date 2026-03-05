// by Claude - local backend for UiTestScope, wraps in-process view tree primitives
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.aidriver.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView

/**
 * [UiTestBackend] that operates on an in-process view tree.
 * Used by [uiTest] for local testing on all platforms.
 */
class LocalUiTestBackend(
    val root: RView,
    val navigator: PageNavigator?,
    private val idle: suspend () -> Unit = {},
) : UiTestBackend {
    override suspend fun snapshot(): UiSnapshot {
        idle()
        return buildSnapshot(root, navigator)
    }

    override suspend fun perform(action: UiAction): ActionDispatchResult {
        val result = dispatchAction(action, root, navigator)
        idle()
        return result
    }

    override suspend fun logs(lines: Int): List<LogEntry> =
        AiDriverLogBuffer.entries(lines)
}
