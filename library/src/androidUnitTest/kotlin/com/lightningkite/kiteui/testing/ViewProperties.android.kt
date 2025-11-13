package com.lightningkite.kiteui.testing

import android.view.View
import android.widget.TextView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput

/**
 * Android implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        return when (val native = view.native) {
            is TextView -> native.text?.toString()
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
        return native.visibility == View.VISIBLE
    }

    actual fun isEnabled(view: RView): Boolean {
        return view.native.isEnabled
    }

    actual fun getHint(view: RView): String? {
        return when (view) {
            is TextInput -> view.hint
            else -> {
                val native = view.native
                if (native is TextView) {
                    native.hint?.toString()
                } else {
                    null
                }
            }
        }
    }
}
