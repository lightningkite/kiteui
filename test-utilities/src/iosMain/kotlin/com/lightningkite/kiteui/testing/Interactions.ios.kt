@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.kiteui.views.direct.FrameLayoutButton
import com.lightningkite.reactive.core.AppScope
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPointMake
import platform.UIKit.*

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

    actual fun typeText(view: RView, text: String) {
        val nativeView = view.native
        if (nativeView is UITextField) {
            val currentText = nativeView.text ?: ""
            nativeView.text = currentText + text
        } else if (nativeView is UITextView) {
            val currentText = nativeView.text ?: ""
            nativeView.text = currentText + text
        } else {
            throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun clearText(view: RView) {
        val nativeView = view.native
        if (nativeView is UITextField) {
            nativeView.text = ""
        } else if (nativeView is UITextView) {
            nativeView.text = ""
        } else {
            throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun setText(view: RView, text: String) {
        val nativeView = view.native
        if (nativeView is UITextField) {
            nativeView.text = text
        } else if (nativeView is UITextView) {
            nativeView.text = text
        } else {
            throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun scroll(view: RView, deltaX: Int, deltaY: Int) {
        val nativeView = view.native
        if (nativeView is UIScrollView) {
            val currentOffset = nativeView.contentOffset
            nativeView.setContentOffset(
                currentOffset.useContents {
                    CGPointMake(this.x + deltaX, this.y + deltaY)
                },
                animated = false
            )
        } else {
            println("Warning: Scroll not supported on this view type")
        }
    }

    actual fun longClick(view: RView) {
        // iOS doesn't have a standard long press simulation in unit tests
        // For now, just trigger a regular click
        println("Warning: Long press not fully implemented on iOS, performing regular click")
        click(view)
    }

    actual fun swipe(view: RView, startX: Float, startY: Float, endX: Float, endY: Float) {
        // iOS swipe simulation would require gesture recognizer simulation
        // For now, just log a warning
        println("Warning: Swipe gesture simulation not yet implemented on iOS")
    }
}
