package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside

/**
 * iOS implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        val nativeView = view.native

        // If it's a UIControl (button, etc), send the touch up inside action
        if (nativeView is UIControl) {
            nativeView.sendActionsForControlEvents(UIControlEventTouchUpInside)
        } else {
            // For other views, we could add gesture recognizer simulation
            // For now, just log that it's not supported
            println("Warning: Click on non-UIControl views not yet supported on iOS")
        }
    }
}
