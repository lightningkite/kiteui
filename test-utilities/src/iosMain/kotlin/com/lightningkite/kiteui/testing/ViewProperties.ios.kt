@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import kotlinx.cinterop.useContents
import platform.UIKit.*

/**
 * iOS implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        // First, try to get text directly from the native view
        when (val nativeView = view.native) {
            is UILabel -> return nativeView.text
            is UITextField -> return nativeView.text
            is UITextView -> return nativeView.text
            is UIButton -> return nativeView.titleLabel?.text
        }

        // For composite views (like KiteUI buttons), search children
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
        val nativeView = view.native
        return when (nativeView) {
            is UIView -> !nativeView.hidden && nativeView.alpha > 0.0
            else -> false
        }
    }

    actual fun isEnabled(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is UIControl -> nativeView.enabled
            else -> true // Views that aren't controls are considered "enabled"
        }
    }

    actual fun isFocused(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is UIView -> nativeView.isFirstResponder()
            else -> false
        }
    }

    actual fun isClickable(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is UIControl -> nativeView.enabled
            is UIView -> nativeView.userInteractionEnabled
            else -> false
        }
    }

    actual fun getContentDescription(view: RView): String? {
        val nativeView = view.native
        return when (nativeView) {
            is UIView -> nativeView.accessibilityLabel
            else -> null
        }
    }

    actual fun getWidth(view: RView): Int {
        val nativeView = view.native
        return when (nativeView) {
            is UIView -> nativeView.frame.useContents { this.size.width.toInt() }
            else -> 0
        }
    }

    actual fun getHeight(view: RView): Int {
        val nativeView = view.native
        return when (nativeView) {
            is UIView -> nativeView.frame.useContents { this.size.height.toInt() }
            else -> 0
        }
    }

    actual fun isChecked(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is UISwitch -> nativeView.on
            // UIButton can be used for checkboxes/radio buttons with selected state
            is UIButton -> nativeView.selected
            else -> false
        }
    }
}
