package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.maybeCalculationContext
import com.lightningkite.kiteui.views.reactiveScope

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioButton = AppCompatRadioButton

actual var RadioButton.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
        native.maybeCalculationContext?.enabledListeners?.value = value
    }
actual val RadioButton.checked: Writable<Boolean>
    get() {
        return native.checked
    }

@ViewDsl
actual inline fun ViewWriter.radioButtonActual(crossinline setup: RadioButton.() -> Unit) {
    return viewElement(factory = ::NRadioButton, wrapper = ::RadioButton) {
        val theme = currentTheme
        transitionNextView = ViewWriter.TransitionNextView.No
        reactiveScope {
            val it = theme()
            CompoundButtonCompat.setButtonTintList(native, ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    it.let { it.iconOverride ?: it.foreground }.closestColor().copy(alpha = 0.75f).colorInt(),
                    it.let { it.iconOverride ?: it.foreground }.colorInt()
                )
            ))
        }
        setup(this)
    }
}