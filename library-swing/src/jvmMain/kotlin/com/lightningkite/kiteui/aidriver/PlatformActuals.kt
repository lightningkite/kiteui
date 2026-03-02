// by Claude - Swing stubs for AI driver platform expects.
// Swing desktop target doesn't support the AI driver yet.
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView

actual suspend fun buildSnapshot(root: RView, navigator: PageNavigator?): UiSnapshot =
    TODO("AI driver snapshot not implemented for Swing")

actual suspend fun dispatchAction(action: UiAction, root: RView, navigator: PageNavigator?): ActionDispatchResult =
    TODO("AI driver action dispatch not implemented for Swing")
