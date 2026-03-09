// by Claude - JVM/SSR action dispatcher: delegates to shared dispatchActionImpl.
// CommonHtml view overrides use direct state manipulation (not DOM events),
// so they work in SSR when the view tree is kept alive.
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView

actual suspend fun dispatchAction(
    action: UiAction,
    root: RView,
    navigator: PageNavigator?
): ActionDispatchResult = dispatchActionImpl(action, root, navigator)
