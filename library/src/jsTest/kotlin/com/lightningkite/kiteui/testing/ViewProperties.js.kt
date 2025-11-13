package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLButtonElement

/**
 * JavaScript/Web implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        return when (val native = view.native) {
            is HTMLInputElement -> native.value
            is HTMLTextAreaElement -> native.value
            is HTMLButtonElement -> native.textContent
            is HTMLElement -> native.textContent
            else -> {
                // Try to get text property via reflection for custom views
                try {
                    val textProperty = view::class.members.find { it.name == "content" }
                    if (textProperty != null) {
                        val content = textProperty.call(view)
                        when (content) {
                            is com.lightningkite.reactive.ReactiveValue<*> -> content.value?.toString()
                            else -> content?.toString()
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    actual fun isVisible(view: RView): Boolean {
        val native = view.native
        if (native !is HTMLElement) return true

        // Check CSS display and visibility
        val style = js("window.getComputedStyle(native)")
        val display = style.display as? String ?: "block"
        val visibility = style.visibility as? String ?: "visible"

        return display != "none" && visibility != "hidden"
    }

    actual fun isEnabled(view: RView): Boolean {
        val native = view.native
        return when (native) {
            is HTMLInputElement -> !native.disabled
            is HTMLButtonElement -> !native.disabled
            is HTMLTextAreaElement -> !native.disabled
            else -> true // Non-form elements don't have a disabled state
        }
    }

    actual fun getHint(view: RView): String? {
        return when (view) {
            is TextInput -> view.hint
            else -> {
                val native = view.native
                when (native) {
                    is HTMLInputElement -> native.placeholder
                    is HTMLTextAreaElement -> native.placeholder
                    else -> null
                }
            }
        }
    }
}
