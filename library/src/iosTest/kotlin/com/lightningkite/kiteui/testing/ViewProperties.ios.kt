package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput
import platform.UIKit.UILabel
import platform.UIKit.UITextField
import platform.UIKit.UIButton
import platform.UIKit.UIControl

/**
 * iOS implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        return when (val native = view.native) {
            is UILabel -> native.text
            is UITextField -> native.text
            is UIButton -> native.titleLabel?.text
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
        return !view.native.hidden
    }

    actual fun isEnabled(view: RView): Boolean {
        val native = view.native
        return if (native is UIControl) {
            native.enabled
        } else {
            // Non-controls are always "enabled" from an interaction perspective
            true
        }
    }

    actual fun getHint(view: RView): String? {
        return when (view) {
            is TextInput -> view.hint
            else -> {
                val native = view.native
                if (native is UITextField) {
                    native.placeholder
                } else {
                    null
                }
            }
        }
    }
}
