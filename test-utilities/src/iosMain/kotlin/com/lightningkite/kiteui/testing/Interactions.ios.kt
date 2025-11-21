package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.kiteui.views.direct.FrameLayoutButton
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.launch
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside

/**
 * iOS implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        // For views with actions (like buttons), trigger the action directly
        if (view is RViewWithAction && view.action != null) {
            AppScope.launch {
                view.action?.startAction(AppScope)
            }
            return
        }

        val nativeView = view.native

        // If it's a FrameLayoutButton, call its onclick method directly
        if (nativeView is FrameLayoutButton) {
            nativeView.onclick()
        } else if (nativeView is UIControl) {
            // For other UIControls, send the touch up inside action
            nativeView.sendActionsForControlEvents(UIControlEventTouchUpInside)
        } else {
            // For other views, we could add gesture recognizer simulation
            // For now, just log that it's not supported
            println("Warning: Click on non-UIControl views not yet supported on iOS")
        }
    }
}
