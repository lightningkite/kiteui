package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.maybeCalculationContext

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NToggleButton(context: Context): SlightlyModifiedFrameLayout(context), View.OnClickListener {
    val checked = Property(false)
    init { setOnClickListener(this) }

    override fun onClick(v: View?) {
        checked.value = !checked.value
    }
}

actual var ToggleButton.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
        native.maybeCalculationContext?.enabledListeners?.value = value
    }
actual val ToggleButton.checked: Writable<Boolean>
    get() = native.checked

@ViewDsl
actual inline fun ViewWriter.toggleButtonActual(crossinline setup: ToggleButton.() -> Unit) {
    return viewElement(factory = ::NToggleButton, wrapper = ::ToggleButton) {
        handleThemeControl(native, checked = { checked.await() }){
            setup(this)
        }
    }
}