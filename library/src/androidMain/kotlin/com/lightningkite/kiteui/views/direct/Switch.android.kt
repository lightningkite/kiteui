package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.maybeCalculationContext
import com.lightningkite.kiteui.views.reactiveScope

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwitch = SwitchCompat

actual var Switch.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
        native.maybeCalculationContext?.enabledListeners?.value = value
    }
actual val Switch.checked: Writable<Boolean>
    get() {
        return native.checked
    }

@ViewDsl
actual inline fun ViewWriter.switchActual(crossinline setup: Switch.() -> Unit) {
    return viewElement(factory = ::SwitchCompat, wrapper = ::Switch) {
        val theme = currentTheme
        reactiveScope {
            val it = theme()
            native.thumbTintList = ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    it.background.closestColor().highlight(.2f).colorInt(),
                    it.important().background.colorInt()
                )
            )
            native.trackTintList = ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    it.background.closestColor().highlight(.1f).colorInt(),
                    it.background.closestColor().highlight(.1f).colorInt(),
                )
            )
        }
        setup(this)
    }
}