package com.lightningkite.kiteui.testing

import com.lightningkite.reactive.core.AppScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.kiteui.views.direct.TextInput
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
