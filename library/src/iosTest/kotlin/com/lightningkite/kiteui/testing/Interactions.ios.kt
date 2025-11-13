package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIScrollView

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

    actual fun typeText(view: RView, text: String, append: Boolean) {
        when (view) {
            is TextInput -> {
                val currentText = view.content.value
                view.content.value = if (append) currentText + text else text
            }
            else -> {
                // Try to find a content property via reflection
                try {
                    val contentProperty = view::class.members.find { it.name == "content" }
                    if (contentProperty != null) {
                        @Suppress("UNCHECKED_CAST")
                        val content = contentProperty.call(view) as? com.lightningkite.reactive.MutableReactiveValue<String>
                        if (content != null) {
                            val currentText = content.value
                            content.value = if (append) currentText + text else text
                        } else {
                            throw IllegalArgumentException("View does not have a MutableReactiveValue<String> content property")
                        }
                    } else {
                        throw IllegalArgumentException("View does not have a content property")
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException("Cannot type text into view of type ${view::class.simpleName}: ${e.message}", e)
                }
            }
        }
    }

    actual fun scrollBy(view: RView, dx: Int, dy: Int) {
        val native = view.native
        if (native is UIScrollView) {
            val currentOffset = native.contentOffset
            val newX = currentOffset.useContents { x + dx }
            val newY = currentOffset.useContents { y + dy }
            native.setContentOffset(CGPointMake(newX, newY), animated = false)
        } else {
            println("Warning: scrollBy not supported for ${native::class.simpleName}")
        }
    }

    actual fun scrollToView(scrollView: RView, targetView: RView) {
        val scrollNative = scrollView.native
        val targetNative = targetView.native

        if (scrollNative is UIScrollView) {
            // Get target frame relative to scroll view
            val targetFrame = targetNative.frame
            scrollNative.scrollRectToVisible(targetFrame, animated = false)
        } else {
            println("Warning: scrollToView not supported for ${scrollNative::class.simpleName}")
        }
    }
}
