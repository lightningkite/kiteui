package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatToggleButton
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.maybeCalculationContext

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NRadioToggleButton(context: Context): SlightlyModifiedFrameLayout(context), View.OnClickListener {
    val checked = Property(false)
    init { setOnClickListener(this) }

    override fun onClick(v: View?) {
        if(!checked.value) checked.value = true
    }
}

actual var RadioToggleButton.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
        native.maybeCalculationContext?.enabledListeners?.value = value
    }
actual val RadioToggleButton.checked: Writable<Boolean>
    get() = native.checked

@ViewDsl
actual inline fun ViewWriter.radioToggleButtonActual(crossinline setup: RadioToggleButton.() -> Unit) {
    return viewElement(factory = ::NRadioToggleButton, wrapper = ::RadioToggleButton) {
        handleThemeControl(native, checked = { checked.await() }){
            setup(this)
        }
    }
}