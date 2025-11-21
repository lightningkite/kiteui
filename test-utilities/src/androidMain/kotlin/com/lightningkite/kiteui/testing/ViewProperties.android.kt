package com.lightningkite.kiteui.testing

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.lightningkite.kiteui.views.RView

/**
 * Android implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        // First, try to get text directly from the native view
        when (val nativeView = view.native) {
            is TextView -> return nativeView.text?.toString()
            is EditText -> return nativeView.text?.toString()
            is Button -> return nativeView.text?.toString()
        }

        // Only search children for specific composite view types (like KiteUI buttons)
        // Check if this is a button-like container (FrameLayout with clickable children)
        if (view is com.lightningkite.kiteui.views.direct.Button) {
            for (child in view.children) {
                val childText = getText(child)
                if (childText != null && childText.isNotEmpty()) {
                    return childText
                }
            }
        }

        return null
    }

    actual fun isVisible(view: RView): Boolean {
        return view.native.visibility == View.VISIBLE
    }

    actual fun isEnabled(view: RView): Boolean {
        return view.native.isEnabled
    }

    actual fun isFocused(view: RView): Boolean {
        return view.native.isFocused
    }

    actual fun isClickable(view: RView): Boolean {
        return view.native.isClickable
    }

    actual fun getContentDescription(view: RView): String? {
        return view.native.contentDescription?.toString()
    }

    actual fun getWidth(view: RView): Int {
        return view.native.width
    }

    actual fun getHeight(view: RView): Int {
        return view.native.height
    }

    actual fun isChecked(view: RView): Boolean {
        return when (val nativeView = view.native) {
            is CheckBox -> nativeView.isChecked
            is RadioButton -> nativeView.isChecked
            is SwitchCompat -> nativeView.isChecked
            else -> false
        }
    }
}
