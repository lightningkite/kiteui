package com.lightningkite.kiteui.testing

import com.lightningkite.reactive.core.AppScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlinx.coroutines.launch

/**
 * JavaScript/Web implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        // If this is a view with an action (like a button), trigger the action directly
        if (view is RViewWithAction && view.action != null) {
            AppScope.launch {
                view.action?.startAction(AppScope)
            }
        }
        // Otherwise could dispatch DOM events, but not needed for buttons
    }
}
