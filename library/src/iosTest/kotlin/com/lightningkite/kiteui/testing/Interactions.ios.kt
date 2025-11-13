package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput
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
}
