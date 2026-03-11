package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.handleCommand

/**
 * In-process test backend that dispatches commands directly to the view tree.
 * Used by platform-specific [uiTest] implementations.
 */
class LocalUiTestBackend(
    val root: () -> RView?,
    val navigator: () -> PageNavigator?,
) : UiTestBackend {
    override suspend fun command(command: String): String =
        handleCommand(command, root(), navigator())
}
