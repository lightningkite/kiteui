package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * JavaScript/Web implementation of ViewProperties.
 */
actual object ViewProperties {
    actual fun getText(view: RView): String? {
        val nativeView = view.native
        return when (nativeView) {
            is HTMLInputElement -> nativeView.value
            is HTMLTextAreaElement -> nativeView.value
            is HTMLButtonElement -> nativeView.textContent
            is HTMLElement -> nativeView.textContent
            else -> null
        }
    }

    actual fun isVisible(view: RView): Boolean {
        val nativeView = view.native as? HTMLElement
        return if (nativeView != null) {
            val style = nativeView.style
            val display = style.display
            val visibility = style.visibility
            val opacity = style.opacity

            display != "none" &&
            visibility != "hidden" &&
            (opacity.isEmpty() || opacity.toDoubleOrNull()?.let { it > 0.0 } ?: true)
        } else {
            false
        }
    }

    actual fun isEnabled(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is HTMLInputElement -> !nativeView.disabled
            is HTMLButtonElement -> !nativeView.disabled
            is HTMLTextAreaElement -> !nativeView.disabled
            else -> true
        }
    }

    actual fun isFocused(view: RView): Boolean {
        val nativeView = view.native as? HTMLElement
        return nativeView != null && nativeView == kotlinx.browser.document.activeElement
    }

    actual fun isClickable(view: RView): Boolean {
        val nativeView = view.native as? HTMLElement
        return if (nativeView != null) {
            when (nativeView) {
                is HTMLButtonElement -> !nativeView.disabled
                is HTMLInputElement -> !nativeView.disabled
                else -> nativeView.onclick != null || nativeView.hasAttribute("onclick")
            }
        } else {
            false
        }
    }

    actual fun getContentDescription(view: RView): String? {
        val nativeView = view.native as? HTMLElement
        return nativeView?.getAttribute("aria-label")
    }

    actual fun getWidth(view: RView): Int {
        val nativeView = view.native as? HTMLElement
        return nativeView?.offsetWidth ?: 0
    }

    actual fun getHeight(view: RView): Int {
        val nativeView = view.native as? HTMLElement
        return nativeView?.offsetHeight ?: 0
    }

    actual fun isChecked(view: RView): Boolean {
        val nativeView = view.native
        return when (nativeView) {
            is HTMLInputElement -> nativeView.checked
            else -> false
        }
    }
}
