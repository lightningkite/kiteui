// by Claude - platform expect declarations for the AI driver client
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView

/**
 * Builds a complete [UiSnapshot] by walking the KiteUI view tree rooted at [root].
 * Called from [AppScope] so already on the main thread.
 */
// by Claude - added settings parameter for hidden element filtering and interactiveOnly mode
expect suspend fun buildSnapshot(root: RView, navigator: PageNavigator?, settings: UiSnapshotSettings = UiSnapshotSettings()): UiSnapshot

/**
 * Dispatches a [UiAction] against the view tree rooted at [root] and returns the result.
 * Called from [AppScope] so already on the main thread.
 */
expect suspend fun dispatchAction(action: UiAction, root: RView, navigator: PageNavigator?): ActionDispatchResult
