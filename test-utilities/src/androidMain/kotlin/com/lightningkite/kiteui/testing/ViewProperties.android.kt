package com.lightningkite.kiteui.testing

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.SimplifiedLinearLayoutLayoutParams
import com.lightningkite.kiteui.views.lparams

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

    actual fun getHorizontalAlign(view: RView): com.lightningkite.kiteui.models.Align? {
        val params = view.lparams
        val gravity = when (params) {
            is SimplifiedLinearLayoutLayoutParams -> params.gravity
            is android.widget.FrameLayout.LayoutParams -> params.gravity
            is CoordinatorLayout.LayoutParams -> params.gravity
            else -> return null
        }

        val masked = gravity and Gravity.HORIZONTAL_GRAVITY_MASK

        return when (masked) {
            Gravity.START and Gravity.HORIZONTAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.Start
            Gravity.CENTER_HORIZONTAL and Gravity.HORIZONTAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.Center
            Gravity.END and Gravity.HORIZONTAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.End
            else -> com.lightningkite.kiteui.models.Align.Center // Default to Center when not explicitly set
        }
    }

    actual fun getVerticalAlign(view: RView): com.lightningkite.kiteui.models.Align? {
        val params = view.lparams
        val gravity = when (params) {
            is SimplifiedLinearLayoutLayoutParams -> params.gravity
            is android.widget.FrameLayout.LayoutParams -> params.gravity
            is CoordinatorLayout.LayoutParams -> params.gravity
            else -> return null
        }

        val masked = gravity and Gravity.VERTICAL_GRAVITY_MASK

        return when (masked) {
            Gravity.TOP and Gravity.VERTICAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.Start
            Gravity.CENTER_VERTICAL and Gravity.VERTICAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.Center
            Gravity.BOTTOM and Gravity.VERTICAL_GRAVITY_MASK -> com.lightningkite.kiteui.models.Align.End
            else -> com.lightningkite.kiteui.models.Align.Center // Default to Center when not explicitly set
        }
    }
}
