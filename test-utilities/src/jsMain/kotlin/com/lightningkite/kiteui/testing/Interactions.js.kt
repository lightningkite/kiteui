package com.lightningkite.kiteui.testing

import com.lightningkite.reactive.core.AppScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

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

    actual fun typeText(view: RView, text: String) {
        val nativeView = view.native
        when (nativeView) {
            is HTMLInputElement -> {
                nativeView.value = nativeView.value + text
            }
            is HTMLTextAreaElement -> {
                nativeView.value = nativeView.value + text
            }
            else -> throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun clearText(view: RView) {
        val nativeView = view.native
        when (nativeView) {
            is HTMLInputElement -> {
                nativeView.value = ""
            }
            is HTMLTextAreaElement -> {
                nativeView.value = ""
            }
            else -> throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun setText(view: RView, text: String) {
        val nativeView = view.native
        when (nativeView) {
            is HTMLInputElement -> {
                nativeView.value = text
            }
            is HTMLTextAreaElement -> {
                nativeView.value = text
            }
            else -> throw IllegalArgumentException("View is not a text input")
        }
    }

    actual fun scroll(view: RView, deltaX: Int, deltaY: Int) {
        val nativeView = view.native as? org.w3c.dom.Element
        if (nativeView != null) {
            nativeView.scrollBy(deltaX.toDouble(), deltaY.toDouble())
        } else {
            println("Warning: Scroll not supported on this view type")
        }
    }

    actual fun longClick(view: RView) {
        // Web doesn't have a standard long click
        // For now, just trigger a regular click
        println("Warning: Long click not fully implemented on Web, performing regular click")
        click(view)
    }

    actual fun swipe(view: RView, startX: Float, startY: Float, endX: Float, endY: Float) {
        // Web swipe simulation would require touch event simulation
        // For now, just log a warning
        println("Warning: Swipe gesture simulation not yet implemented on Web")
    }
}
